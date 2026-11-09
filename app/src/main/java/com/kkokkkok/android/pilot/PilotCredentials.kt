package com.kkokkkok.android.pilot

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.kkokkkok.android.BuildConfig
import java.net.HttpURLConnection
import java.net.URI
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/** Per-server installation credential. Raw tokens never enter build configuration or logs. */
object PilotCredentials {
    private var appContext: Context? = null
    private var session: JSONObject? = null
    private val mutableGeneration = MutableStateFlow(0)
    val generation = mutableGeneration.asStateFlow()
    private val scope = MessageDigest.getInstance("SHA-256")
        .digest(BuildConfig.BACKEND_BASE_URL.trimEnd('/').toByteArray()).joinToString("") { "%02x".format(it) }
    private val alias = "kkokkkok-pilot-$scope"

    @Synchronized fun initialize(context: Context) {
        appContext = context.applicationContext
        session = runCatching {
            val saved = preferences().getString("session", null) ?: return@runCatching null
            val parts = saved.split(':')
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)))
            cipher.updateAAD(scope.toByteArray())
            JSONObject(String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), Charsets.UTF_8))
        }.getOrNull()
    }
    @Synchronized fun installationId(): String? = session?.optString("installationId")?.takeIf { it.isNotBlank() }
    @Synchronized fun hasSession(): Boolean = session != null
    @Synchronized fun save(value: JSONObject) {
        require(value.getString("token").matches(Regex("kkp_[A-Za-z0-9_-]{43}")))
        java.util.UUID.fromString(value.getString("installationId"))
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        cipher.updateAAD(scope.toByteArray())
        val encrypted = cipher.doFinal(value.toString().toByteArray())
        val encoded = Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" + Base64.encodeToString(encrypted, Base64.NO_WRAP)
        check(preferences().edit().putString("session", encoded).commit()) { "기기 등록 정보를 보관하지 못했어요." }
        session = value
    }
    @Synchronized fun invalidate() {
        if (session == null) return
        session = null
        preferences().edit().remove("session").apply()
        mutableGeneration.value += 1
    }
    @Synchronized fun authorize(connection: HttpURLConnection) {
        if (!sameOrigin(connection)) return
        val value = session ?: return
        connection.setRequestProperty("Authorization", "Bearer ${value.getString("token")}")
        connection.setRequestProperty("X-Installation-Id", value.getString("installationId"))
        // Do not forward credentials through an HTTP redirect.
        connection.instanceFollowRedirects = false
    }
    fun responseCode(connection: HttpURLConnection): Int = connection.responseCode.also {
        if (it == 401 && sameOrigin(connection) && BuildConfig.PILOT_ACCESS_REQUIRED) invalidate()
    }
    private fun sameOrigin(connection: HttpURLConnection): Boolean {
        val base = URI(BuildConfig.BACKEND_BASE_URL)
        val target = connection.url.toURI()
        return base.scheme == target.scheme && base.host == target.host && base.port == target.port
    }
    private fun preferences() = requireNotNull(appContext).getSharedPreferences("pilot_credentials_$scope", Context.MODE_PRIVATE)
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
