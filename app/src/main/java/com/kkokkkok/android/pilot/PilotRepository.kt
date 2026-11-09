package com.kkokkkok.android.pilot

import com.kkokkkok.android.BuildConfig
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class PilotRepository {
    suspend fun validate(): Boolean = withContext(Dispatchers.IO) {
        if (!PilotCredentials.hasSession()) return@withContext false
        request("/api/installations/current", null).getString("installationId") == PilotCredentials.installationId()
    }
    suspend fun enroll(code: String) = withContext(Dispatchers.IO) {
        val result = request("/api/installations/enroll", JSONObject().put("inviteCode", code.trim()))
        PilotCredentials.save(result)
    }
    private fun request(path: String, body: JSONObject?): JSONObject {
        if (!BuildConfig.DEBUG && !BuildConfig.BACKEND_BASE_URL.startsWith("https://")) throw IOException("파일럿 서버는 HTTPS 주소가 필요해요.")
        val connection = URL(BuildConfig.BACKEND_BASE_URL.trimEnd('/') + path).openConnection() as HttpURLConnection
        try {
            connection.instanceFollowRedirects = false
            connection.requestMethod = if (body == null) "GET" else "POST"
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            if (body == null) PilotCredentials.authorize(connection)
            else {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write(body.toString().toByteArray()) }
            }
            val status = PilotCredentials.responseCode(connection)
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status == 401) return JSONObject().put("installationId", "")
            if (status !in 200..299) throw IOException(
                runCatching { JSONObject(text).getJSONObject("error").getString("message") }.getOrNull()
                    ?: "서버에 연결하지 못했어요. 잠시 후 다시 시도해 주세요.")
            return JSONObject(text)
        } finally { connection.disconnect() }
    }
}
