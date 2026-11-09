package com.kkokkkok.android.protection

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

data class ProtectionPreview(
    val editedPhoto: Uri,
    val isMock: Boolean,
    val mockNotice: String?,
    val providerVersion: String?,
    val verificationStatus: String?,
    val verificationScore: Int?,
    val verificationCueCount: Int?,
    val verificationInspectionId: String? = null,
)

interface ProtectionEditRepository {
    suspend fun refreshVerification(preview: ProtectionPreview): ProtectionPreview = preview
    suspend fun createEdit(
        inspectionId: String,
        cueIds: List<String>,
        preservePrompt: String,
        editStrength: Int,
        idempotencyKey: String,
        sourcePhoto: Uri,
    ): ProtectionPreview
}

/** Calls the app backend; image editing provider keys stay on the server. */
class BackendProtectionEditRepository(
    context: Context,
    baseUrl: String,
) : ProtectionEditRepository {
    private val appContext = context.applicationContext
    private val baseUrl = baseUrl.trimEnd('/')
    private val editCache = File(appContext.cacheDir, "protection-edits")

    init { require(this.baseUrl.startsWith("http://") || this.baseUrl.startsWith("https://")) }

    override suspend fun createEdit(
        inspectionId: String,
        cueIds: List<String>,
        preservePrompt: String,
        editStrength: Int,
        idempotencyKey: String,
        sourcePhoto: Uri,
    ): ProtectionPreview = withContext(Dispatchers.IO) {
        if (inspectionId.isBlank()) throw IOException("먼저 실제 사진 검사를 완료해 주세요.")
        if (cueIds.isEmpty()) throw IOException("선택한 사진에서 편집할 수 있는 단서를 찾지 못했어요.")
        val request = JSONObject()
            .put("inspectionId", inspectionId)
            .put("cueIds", JSONArray(cueIds))
            .put("preservePrompt", preservePrompt)
            .put("editStrength", editStrength.coerceIn(0, 100))
        var response = requestJson("/api/edits", "POST", request, idempotencyKey)
        var polls = 0
        while (response.optString("status") in setOf("queued", "processing", "submitting")) {
            if (polls >= 180) throw IOException("사진 편집 결과를 기다리는 시간이 초과됐어요. 다시 시도하면 같은 편집 작업을 확인합니다.")
            delay(1_000)
            response = getJson("/api/edits/$idempotencyKey")
            polls += 1
        }
        if (response.optString("status") == "failed") {
            throw IOException("사진 편집에 실패했어요. 단서 영역이나 요청을 확인하고 다시 시도해 주세요.")
        }
        if (response.optString("status") != "completed") {
            throw IOException("사진 편집을 완료하지 못했어요. 다시 시도해 주세요.")
        }
        val editedAssetId = response.optString("editedAssetId")
        if (editedAssetId.isBlank()) throw IOException("수정된 사진 정보를 받지 못했어요.")
        val editedPhoto = cacheAsset(editedAssetId)
        val verification = response.optString("verificationStatus").takeIf(String::isNotBlank)
        val verificationSummary = response.optString("verificationInspectionId")
            .takeIf(String::isNotBlank)
            ?.let { runCatching { getJson("/api/inspections/$it") }.getOrNull() }
        val riskSummary = verificationSummary?.optJSONObject("riskSummary")
        val score = (riskSummary?.opt("score") as? Number)?.toInt()?.takeIf { it in 0..100 }
        val cueCount = (verificationSummary?.optJSONArray("riskFactors")?.length())
        ProtectionPreview(
            editedPhoto = editedPhoto,
            isMock = response.optBoolean("mock", false),
            mockNotice = response.optString("mockNotice").takeIf(String::isNotBlank),
            providerVersion = response.optString("providerVersion").takeIf(String::isNotBlank),
            verificationStatus = if (verificationSummary?.optBoolean("mock") == true) "mock" else verificationSummary?.optString("status")?.takeIf(String::isNotBlank) ?: verification,
            verificationScore = if (verificationSummary?.optBoolean("mock") == true) null else score,
            verificationCueCount = if (verificationSummary?.optBoolean("mock") == true) null else cueCount,
            verificationInspectionId = response.optString("verificationInspectionId").takeIf { it.isNotBlank() && it != "null" },
        )
    }

    override suspend fun refreshVerification(preview: ProtectionPreview): ProtectionPreview = withContext(Dispatchers.IO) {
        val id = preview.verificationInspectionId ?: return@withContext preview
        val response = getJson("/api/inspections/$id")
        if (response.optBoolean("mock")) return@withContext preview.copy(
            verificationStatus = "mock", verificationScore = null, verificationCueCount = null)
        val result = (com.kkokkkok.android.analysis.parseInspectionState(response)
            as? com.kkokkkok.android.analysis.AnalysisState.Completed)?.result
        preview.copy(verificationStatus = response.optString("status"), verificationScore = result?.score,
            verificationCueCount = result?.clues?.count { it.risk == com.kkokkkok.android.analysis.ClueRisk.HIGH || it.risk == com.kkokkkok.android.analysis.ClueRisk.CAUTION })
    }

    private fun cacheAsset(assetId: String): Uri {
        editCache.mkdirs()
        val target = File(editCache, "$assetId.image")
        if (!target.isFile || target.length() == 0L) {
            val connection = openConnection("/api/assets/$assetId/content", "GET")
            try {
                if (connection.responseCode !in 200..299) throw errorFrom(connection)
                val temporary = File(editCache, "$assetId.tmp")
                connection.inputStream.use { input -> temporary.outputStream().use(input::copyTo) }
                if (!temporary.renameTo(target)) {
                    temporary.copyTo(target, overwrite = true)
                    temporary.delete()
                }
            } finally { connection.disconnect() }
        }
        return Uri.fromFile(target)
    }

    private fun getJson(path: String): JSONObject {
        val connection = openConnection(path, "GET")
        try { return readJson(connection) } finally { connection.disconnect() }
    }

    private fun requestJson(path: String, method: String, body: JSONObject, idempotencyKey: String): JSONObject {
        val connection = openConnection(path, method)
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        connection.setRequestProperty("Idempotency-Key", idempotencyKey)
        connection.doOutput = true
        try {
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            return readJson(connection)
        } finally { connection.disconnect() }
    }

    private fun readJson(connection: HttpURLConnection): JSONObject {
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        if (code !in 200..299) {
            val message = runCatching { JSONObject(body).getJSONObject("error").getString("message") }
                .getOrNull() ?: "서버와 통신하지 못했어요. 잠시 후 다시 시도해 주세요."
            throw IOException(message)
        }
        return runCatching { JSONObject(body) }.getOrElse { throw IOException("서버 응답을 읽을 수 없어요.") }
    }

    private fun errorFrom(connection: HttpURLConnection): IOException {
        val code = connection.responseCode
        val body = connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        val message = runCatching { JSONObject(body).getJSONObject("error").getString("message") }.getOrNull()
        return IOException(message ?: "수정된 사진을 불러오지 못했어요. (HTTP $code)")
    }

    private fun openConnection(path: String, method: String) =
        (URL("$baseUrl$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 10_000
            readTimeout = if (path == "/api/edits") 300_000 else 30_000
            setRequestProperty("Accept", "application/json, image/*")
        }
}

/** Explicitly marked fallback for previews and isolated UI tests only. */
class MockProtectionEditRepository : ProtectionEditRepository {
    override suspend fun createEdit(
        inspectionId: String,
        cueIds: List<String>,
        preservePrompt: String,
        editStrength: Int,
        idempotencyKey: String,
        sourcePhoto: Uri,
    ): ProtectionPreview {
        delay(450)
        return ProtectionPreview(sourcePhoto, true, "모의 편집입니다. 사진 내용은 실제로 바뀌지 않았어요.",
            "test-copy-v1", "unavailable", null, null)
    }
}
