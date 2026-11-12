package com.kkokkkok.android.account

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.kkokkkok.android.BuildConfig
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.security.KeyStore
import java.security.MessageDigest
import java.time.Instant
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/** Service login tokens issued by the backend after Kakao verification. Never logged. */
data class AuthTokens(
    val accessToken: String,
    val accessExpiresAt: Instant,
    val refreshToken: String,
    val refreshExpiresAt: Instant,
    val userId: String,
    val displayName: String,
) {
    override fun toString() = "AuthTokens[redacted]"

    fun toJson(): JSONObject = JSONObject()
        .put("accessToken", accessToken).put("accessTokenExpiresAt", accessExpiresAt.toString())
        .put("refreshToken", refreshToken).put("refreshTokenExpiresAt", refreshExpiresAt.toString())
        .put("user", JSONObject().put("userId", userId).put("displayName", displayName))

    companion object {
        private val ACCESS = Regex("kka_[A-Za-z0-9_-]{43}")
        private val REFRESH = Regex("kkr_[A-Za-z0-9_-]{43}")

        /** Parses the backend login/refresh response; rejects anything that does not look like service tokens. */
        fun parse(json: JSONObject): AuthTokens {
            val user = json.getJSONObject("user")
            val access = json.getString("accessToken")
            val refresh = json.getString("refreshToken")
            require(access.matches(ACCESS) && refresh.matches(REFRESH)) { "Invalid token format" }
            java.util.UUID.fromString(user.getString("userId"))
            return AuthTokens(
                access, Instant.parse(json.getString("accessTokenExpiresAt")),
                refresh, Instant.parse(json.getString("refreshTokenExpiresAt")),
                user.getString("userId"), user.optString("displayName").ifBlank { "꼭꼭 사용자" },
            )
        }
    }
}

/**
 * Encrypted (Android Keystore AES-GCM) per-server login session. All API repositories reach it through
 * [com.kkokkkok.android.pilot.PilotCredentials.authorize] / responseCode when [BuildConfig.LOGIN_REQUIRED].
 */
object AccountSession {
    private const val REFRESH_MARGIN_SECONDS = 60L
    private var appContext: Context? = null
    private var tokens: AuthTokens? = null
    private val mutableGeneration = MutableStateFlow(0)
    /** Increments whenever the signed-in user changes or the session ends; screens reset their state. */
    val generation = mutableGeneration.asStateFlow()
    private val scope = MessageDigest.getInstance("SHA-256")
        .digest(BuildConfig.BACKEND_BASE_URL.trimEnd('/').toByteArray()).joinToString("") { "%02x".format(it) }
    private val alias = "kkokkkok-account-$scope"
    /** Set by the signup completion "장소 등록하기" action; consumed once by the main app. */
    @Volatile var openPlaceAddOnStart: Boolean = false

    @Synchronized fun initialize(context: Context) {
        appContext = context.applicationContext
        tokens = runCatching {
            val saved = preferences().getString("session", null) ?: return@runCatching null
            val parts = saved.split(':')
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)))
            cipher.updateAAD(scope.toByteArray())
            AuthTokens.parse(JSONObject(String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), Charsets.UTF_8)))
        }.getOrNull()?.takeIf { it.refreshExpiresAt.isAfter(Instant.now()) }
    }

    @Synchronized fun current(): AuthTokens? = tokens
    @Synchronized fun isSignedIn(): Boolean = tokens != null

    @Synchronized fun save(value: AuthTokens) {
        val previousUser = tokens?.userId
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        cipher.updateAAD(scope.toByteArray())
        val encrypted = cipher.doFinal(value.toJson().toString().toByteArray())
        val encoded = Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" + Base64.encodeToString(encrypted, Base64.NO_WRAP)
        check(preferences().edit().putString("session", encoded).commit()) { "로그인 정보를 보관하지 못했어요." }
        tokens = value
        if (previousUser != value.userId) mutableGeneration.value += 1
    }

    @Synchronized fun clear() {
        if (tokens == null) return
        tokens = null
        preferences().edit().remove("session").apply()
        mutableGeneration.value += 1
    }

    @Synchronized fun termsAccepted(userId: String): Boolean = preferences().getBoolean("terms.$userId", false)
    @Synchronized fun acceptTerms(userId: String) { preferences().edit().putBoolean("terms.$userId", true).apply() }

    /** Called on a background thread right before an API request. Refreshes the access token when near expiry. */
    fun authorize(connection: HttpURLConnection) {
        if (!sameOrigin(connection.url)) return
        val value = freshTokens() ?: return
        connection.setRequestProperty("Authorization", "Bearer ${value.accessToken}")
        // Do not forward credentials through an HTTP redirect.
        connection.instanceFollowRedirects = false
    }

    /** On 401 tries one refresh; if the session cannot be renewed the user is sent back to login. */
    fun responseCode(connection: HttpURLConnection): Int = connection.responseCode.also { code ->
        if (code == 401 && sameOrigin(connection.url) && tokens != null) {
            if (!refreshBlocking(force = true)) clear()
        }
    }

    private fun freshTokens(): AuthTokens? {
        val value = current() ?: return null
        if (value.accessExpiresAt.isAfter(Instant.now().plusSeconds(REFRESH_MARGIN_SECONDS))) return value
        refreshBlocking(force = false)
        return current()
    }

    private val refreshLock = Any()

    /** Serialized so concurrent requests do not rotate the refresh token twice. */
    private fun refreshBlocking(force: Boolean): Boolean = synchronized(refreshLock) {
        val value = current() ?: return false
        if (!force && value.accessExpiresAt.isAfter(Instant.now().plusSeconds(REFRESH_MARGIN_SECONDS))) return true
        if (value.refreshExpiresAt.isBefore(Instant.now())) { clear(); return false }
        try {
            val response = AuthApi.post("/api/auth/refresh", JSONObject().put("refreshToken", value.refreshToken))
            when (response.code) {
                in 200..299 -> { save(AuthTokens.parse(response.body)); true }
                401 -> { clear(); false }
                else -> false
            }
        } catch (_: Exception) {
            false // Network failure: keep the session; the request reports its own error and can be retried.
        }
    }

    internal fun sameOrigin(url: URL): Boolean {
        val base = URI(BuildConfig.BACKEND_BASE_URL)
        val target = url.toURI()
        return base.scheme == target.scheme && base.host == target.host && base.port == target.port
    }

    private fun preferences() = requireNotNull(appContext).getSharedPreferences("account_session_$scope", Context.MODE_PRIVATE)

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build())
        }.generateKey()
    }
}

/** Minimal JSON client for unauthenticated auth endpoints (login/refresh) and logout. */
internal object AuthApi {
    data class Response(val code: Int, val body: JSONObject)

    fun post(path: String, body: JSONObject?, bearer: String? = null): Response {
        val connection = URL(BuildConfig.BACKEND_BASE_URL.trimEnd('/') + path).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 10_000
            connection.readTimeout = 20_000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", "application/json")
            bearer?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write(body.toString().toByteArray()) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            Response(code, if (text.isBlank()) JSONObject() else runCatching { JSONObject(text) }.getOrDefault(JSONObject()))
        } finally {
            connection.disconnect()
        }
    }
}
