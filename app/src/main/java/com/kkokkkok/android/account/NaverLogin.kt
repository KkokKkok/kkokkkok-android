package com.kkokkkok.android.account

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.browser.customtabs.CustomTabsIntent
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import kotlinx.coroutines.CompletableDeferred

/** PKCE-style secret that proves the app which started a Naver login is the one finishing it (RFC 7636 S256). */
internal object LoginVerifier {
    fun create(): String {
        val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun challenge(verifier: String): String = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)))
}

/** Opens the server-provided Naver login page and waits for the app link. Replaceable in UI tests. */
interface NaverAuthorizer {
    /** Returns the return link (kkokkkok://naver-login?…), or null when the user closed the page. Throws if no browser. */
    suspend fun authorize(context: Context, authorizeUrl: String): Uri?
}

object CustomTabNaverAuthorizer : NaverAuthorizer {
    override suspend fun authorize(context: Context, authorizeUrl: String): Uri? {
        val result = NaverLoginBridge.begin()
        CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(context, Uri.parse(authorizeUrl))
        NaverLoginBridge.opened()
        return result.await()
    }
}

/** Main-thread hand-off between the browser return activity and the suspended login call. */
object NaverLoginBridge {
    private var pending: CompletableDeferred<Uri?>? = null
    private var waitingForReturn = false
    private val main = Handler(Looper.getMainLooper())

    fun begin(): CompletableDeferred<Uri?> {
        pending?.complete(null)
        waitingForReturn = false
        return CompletableDeferred<Uri?>().also { pending = it }
    }

    fun opened() { waitingForReturn = true }

    fun deliver(uri: Uri) {
        waitingForReturn = false
        pending?.complete(uri)
        pending = null
    }

    private var resumed = false
    private var resumeCount = 0

    /**
     * The app is back in front. If it stays in front without a return link, the user closed the login page: cancel.
     * Opening the tab can briefly resume the app (browser trampoline), so the app must stay resumed for the delay.
     */
    fun appResumed() {
        resumed = true
        val token = ++resumeCount
        if (!waitingForReturn) return
        val current = pending ?: return
        main.postDelayed({
            if (resumed && resumeCount == token && waitingForReturn && pending === current) {
                waitingForReturn = false
                current.complete(null)
                pending = null
            }
        }, 1_000)
    }

    fun appPaused() { resumed = false }
}

/** Receives kkokkkok://naver-login, hands it to the waiting login, and returns to the app (closing the browser tab). */
class NaverLoginReturnActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent?.data?.let(NaverLoginBridge::deliver)
        startActivity(Intent(this, com.kkokkkok.android.MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        finish()
    }
}
