package com.kkokkkok.android.account

import android.content.Context
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import com.kkokkkok.android.BuildConfig
import com.kkokkkok.android.network.InstallationIdentity
import java.io.IOException
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject

sealed interface LoginResult {
    data class SignedIn(
        val newUser: Boolean,
        val userId: String,
        val termsAccepted: Boolean,
        val profileConsentRequired: Boolean = false,
    ) : LoginResult
    data object Cancelled : LoginResult
    data class Failed(val message: String) : LoginResult
}

/** Real social login: provider SDK → backend verification → encrypted service session. */
interface LoginRepository {
    suspend fun signIn(provider: SignInProvider, context: Context): LoginResult
    /** Records consent on the server. Returns null on success or a user-facing error message. */
    suspend fun agreeTerms(agreements: AgreementState): String?
    suspend fun logout()
}

/** Obtains a Kakao user access token. Separated so UI tests can substitute a fake. */
interface KakaoTokenSource {
    /** Returns the access token, null when the user cancelled. Throws on failure. */
    suspend fun accessToken(context: Context): String?
    /** Kakao additional consent for optional items (e.g. profile_nickname); returns a new token or null. */
    suspend fun additionalConsent(context: Context, scopes: List<String>): String? = null
}

object KakaoSdkTokenSource : KakaoTokenSource {
    override suspend fun additionalConsent(context: Context, scopes: List<String>): String? =
        suspendCancellableCoroutine { continuation ->
            UserApiClient.instance.loginWithNewScopes(context, scopes) { token, _ ->
                // Declining or failing is not fatal: the app keeps the default display name.
                if (continuation.isActive) continuation.resume(token?.accessToken)
            }
        }

    override suspend fun accessToken(context: Context): String? = suspendCancellableCoroutine { continuation ->
        val client = UserApiClient.instance
        fun finish(token: String?, error: Throwable?) {
            if (!continuation.isActive) return
            when {
                token != null -> continuation.resume(token)
                error is ClientError && error.reason == ClientErrorCause.Cancelled -> continuation.resume(null)
                else -> continuation.resumeWith(Result.failure(error ?: IOException("Kakao login failed")))
            }
        }
        val accountLogin = { client.loginWithKakaoAccount(context) { token, error -> finish(token?.accessToken, error) } }
        if (client.isKakaoTalkLoginAvailable(context)) {
            client.loginWithKakaoTalk(context) { token, error ->
                // Cancelling in KakaoTalk ends the attempt; other KakaoTalk errors fall back to Kakao account login.
                if (token != null || (error is ClientError && error.reason == ClientErrorCause.Cancelled)) finish(token?.accessToken, error)
                else accountLogin()
            }
        } else {
            accountLogin()
        }
    }
}

class BackendLoginRepository(
    private val identity: InstallationIdentity,
    private val kakao: KakaoTokenSource = KakaoSdkTokenSource,
    private val naver: NaverAuthorizer = CustomTabNaverAuthorizer,
) : LoginRepository {
    override suspend fun signIn(provider: SignInProvider, context: Context): LoginResult =
        if (provider == SignInProvider.NAVER) signInNaver(context) else signInKakao(context)

    /**
     * Naver: the server holds the client secret and exchanges the authorization code. The app only opens the login
     * page, receives a one-time code through kkokkkok://naver-login, and redeems it with its verifier.
     */
    private suspend fun signInNaver(context: Context): LoginResult {
        val verifier = LoginVerifier.create()
        val start = withContext(Dispatchers.IO) {
            runCatching {
                AuthApi.post("/api/auth/naver/start", JSONObject()
                    .put("codeChallenge", LoginVerifier.challenge(verifier)).put("installationId", identity.get()))
            }.getOrNull()
        } ?: return LoginResult.Failed("서버에 연결하지 못했어요. 연결을 확인하고 다시 시도해 주세요.")
        val authorizeUrl = start.body.optString("authorizeUrl")
        // Only HTTPS login pages; debug builds also allow a loopback mock (local verification without real Naver keys).
        val allowedUrl = authorizeUrl.startsWith("https://") ||
            (BuildConfig.DEBUG && Regex("http://(127\\.0\\.0\\.1|localhost):\\d+/.*").matches(authorizeUrl))
        if (start.code !in 200..299 || !allowedUrl) return LoginResult.Failed(when (start.code) {
            503 -> "네이버 로그인이 설정되지 않았어요."
            429 -> "로그인 요청이 많아요. 잠시 후 다시 시도해 주세요."
            else -> "로그인 서버에 문제가 생겼어요. 잠시 후 다시 시도해 주세요."
        })
        val link = try {
            naver.authorize(context, authorizeUrl) ?: return LoginResult.Cancelled
        } catch (_: Exception) {
            return LoginResult.Failed("네이버 로그인 화면을 열지 못했어요. 브라우저가 설치되어 있는지 확인해 주세요.")
        }
        when (link.getQueryParameter("error")) {
            null -> Unit
            "cancelled" -> return LoginResult.Cancelled
            "expired" -> return LoginResult.Failed("네이버 로그인 시간이 지났어요. 다시 시도해 주세요.")
            else -> return LoginResult.Failed("네이버 로그인에 실패했어요. 잠시 후 다시 시도해 주세요.")
        }
        val code = link.getQueryParameter("code") ?: return LoginResult.Failed("네이버 로그인에 실패했어요. 잠시 후 다시 시도해 주세요.")
        return complete("/api/auth/naver/exchange", JSONObject().put("code", code).put("codeVerifier", verifier),
            "네이버 로그인 시간이 지났어요. 다시 시도해 주세요.")
    }

    private suspend fun signInKakao(context: Context): LoginResult {
        if (BuildConfig.KAKAO_NATIVE_APP_KEY.isBlank()) return LoginResult.Failed("카카오 로그인이 설정되지 않았어요.")
        val kakaoToken = try {
            kakao.accessToken(context) ?: return LoginResult.Cancelled
        } catch (_: Exception) {
            return LoginResult.Failed("카카오 로그인에 실패했어요. 잠시 후 다시 시도해 주세요.")
        }
        val first = exchange(kakaoToken)
        if (first !is LoginResult.SignedIn || !first.profileConsentRequired) return first
        // Kakao withheld the nickname because the optional consent was not given; ask once, then log in again.
        val consented = runCatching { kakao.additionalConsent(context, listOf("profile_nickname")) }.getOrNull() ?: return first
        val firstAccess = AccountSession.current()?.accessToken
        val second = exchange(consented)
        if (second !is LoginResult.SignedIn) return first
        // End the session created by the first exchange; the second one replaced it locally.
        if (firstAccess != null) withContext(Dispatchers.IO) { runCatching { AuthApi.post("/api/auth/logout", null, firstAccess) } }
        return second.copy(newUser = first.newUser)
    }

    private suspend fun exchange(kakaoToken: String): LoginResult =
        complete("/api/auth/kakao", JSONObject().put("accessToken", kakaoToken).put("installationId", identity.get()),
            "카카오 로그인 정보를 확인할 수 없어요. 다시 시도해 주세요.")

    /** Sends the provider proof to the backend and stores the returned service session. */
    private suspend fun complete(path: String, body: JSONObject, invalidMessage: String): LoginResult {
        return withContext(Dispatchers.IO) {
            try {
                val response = AuthApi.post(path, body)
                when (response.code) {
                    in 200..299 -> {
                        val tokens = AuthTokens.parse(response.body)
                        AccountSession.save(tokens)
                        val accepted = response.body.optBoolean("termsAccepted", false)
                        AccountSession.setTermsAccepted(tokens.userId, accepted)
                        LoginResult.SignedIn(response.body.optBoolean("newUser", false), tokens.userId, accepted,
                            response.body.optBoolean("profileConsentRequired", false))
                    }
                    401 -> LoginResult.Failed(invalidMessage)
                    else -> LoginResult.Failed("로그인 서버에 문제가 생겼어요. 잠시 후 다시 시도해 주세요.")
                }
            } catch (_: Exception) {
                LoginResult.Failed("서버에 연결하지 못했어요. 연결을 확인하고 다시 시도해 주세요.")
            }
        }
    }

    override suspend fun agreeTerms(agreements: AgreementState): String? = withContext(Dispatchers.IO) {
        val session = AccountSession.current() ?: return@withContext "로그인이 필요해요. 다시 로그인해 주세요."
        try {
            // Ask the server which terms version is current so the consent matches what it enforces.
            val status = AuthApi.get("/api/auth/terms", session.accessToken)
            if (status.code == 401) { AccountSession.clear(); return@withContext "로그인이 만료되었어요. 다시 로그인해 주세요." }
            if (status.code !in 200..299) return@withContext "약관 정보를 불러오지 못했어요. 다시 시도해 주세요."
            val response = AuthApi.post("/api/auth/terms", JSONObject()
                .put("termsVersion", status.body.getString("currentVersion"))
                .put("serviceTerms", agreements.serviceTerms).put("privacy", agreements.privacy)
                .put("location", agreements.location).put("marketing", agreements.marketing), session.accessToken)
            when (response.code) {
                in 200..299 -> { AccountSession.setTermsAccepted(session.userId, true); null }
                400 -> "필수 약관에 모두 동의해 주세요."
                409 -> "약관이 변경되었어요. 다시 시도해 주세요."
                else -> "동의 내용을 저장하지 못했어요. 다시 시도해 주세요."
            }
        } catch (_: Exception) {
            "서버에 연결하지 못했어요. 연결을 확인하고 다시 시도해 주세요."
        }
    }

    override suspend fun logout() {
        val access = AccountSession.current()?.accessToken
        withContext(Dispatchers.IO) {
            // Best effort: the local session is cleared even if the server or Kakao cannot be reached.
            // Naver keeps no token on the device (the server deletes it right after login), so nothing to sign out there.
            if (access != null) runCatching { AuthApi.post("/api/auth/logout", null, access) }
        }
        runCatching { suspendCancellableCoroutine { c -> UserApiClient.instance.logout { c.resume(Unit) } } }
        AccountSession.clear()
    }
}
