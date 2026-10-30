package com.kkokkkok.android.analysis

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/** Connects the current single-photo flow to the backend's draft asset and inspection APIs. */
class BackendAnalysisRepository(
    private val contentResolver: ContentResolver,
    baseUrl: String,
    private val pollIntervalMillis: Long = 500,
    private val maxPollCount: Int = 60,
) : AnalysisRepository {
    private val baseUrl = baseUrl.trimEnd('/')
    private val uploadMutex = Mutex()
    private var cachedUri: Uri? = null
    private var cachedUpload: UploadedAsset? = null

    init {
        require(this.baseUrl.startsWith("http://") || this.baseUrl.startsWith("https://"))
        require(pollIntervalMillis >= 0)
        require(maxPollCount > 0)
    }

    override fun analyze(photo: Uri, location: String): Flow<AnalysisState> = flow {
        emit(AnalysisState.Running(AnalysisStep.METADATA, 5))
        val asset = getOrUploadAsset(photo)

        emit(AnalysisState.Running(AnalysisStep.TEXT, 35))
        val idempotencyKey = UUID.randomUUID().toString()
        var result = createInspection(asset.assetId, idempotencyKey, location.takeIf(String::isNotBlank))
        var polls = 0

        while (result.optString("status") == "queued" || result.optString("status") == "processing") {
            if (polls >= maxPollCount) throw IOException("분석 결과를 기다리는 시간이 초과됐어요.")
            delay(pollIntervalMillis)
            result = getInspection(result.optString("inspectionId"))
            polls += 1
            val progress = (45 + polls * 2).coerceAtMost(95)
            emit(AnalysisState.Running(if (polls < 3) AnalysisStep.SIGNS else AnalysisStep.BUILDINGS, progress))
        }

        when (result.optString("status")) {
            "completed" -> emit(AnalysisState.Completed(result.toAnalysisResult()))
            "failed" -> throw IOException("분석을 완료하지 못했어요. 다시 시도해 주세요.")
            else -> throw IOException("분석 서버 응답을 확인할 수 없어요.")
        }
    }

    override suspend fun hasExifGps(photo: Uri): Boolean = getOrUploadAsset(photo).locationStatus == "available"

    private suspend fun getOrUploadAsset(uri: Uri): UploadedAsset = uploadMutex.withLock {
        if (cachedUri == uri) cachedUpload?.let { return it }
        uploadAsset(uri).also {
            cachedUri = uri
            cachedUpload = it
        }
    }

    private suspend fun uploadAsset(uri: Uri): UploadedAsset = withContext(Dispatchers.IO) {
        val mimeType = contentResolver.getType(uri)?.substringBefore(';') ?: "image/jpeg"
        val extension = when (mimeType) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> throw IOException("JPEG, PNG 또는 WebP 사진을 선택해 주세요.")
        }
        val boundary = "KkokKkok-${UUID.randomUUID()}"
        val connection = openConnection("/api/assets", "POST")
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        connection.doOutput = true
        connection.setChunkedStreamingMode(0)

        try {
            BufferedOutputStream(connection.outputStream).use { output ->
                output.write("--$boundary\r\n".toByteArray())
                output.write("Content-Disposition: form-data; name=\"photo\"; filename=\"photo.$extension\"\r\n".toByteArray())
                output.write("Content-Type: $mimeType\r\n\r\n".toByteArray())
                val input = contentResolver.openInputStream(uri)
                    ?: throw IOException("선택한 사진을 읽을 수 없어요.")
                input.use { it.copyTo(output) }
                output.write("\r\n--$boundary--\r\n".toByteArray())
            }
            val response = readJsonResponse(connection)
            val assetId = response.optString("assetId").takeIf(String::isNotBlank)
                ?: throw IOException("사진 저장 응답을 확인할 수 없어요.")
            val locationStatus = response.optString("locationStatus").takeIf { it in setOf("available", "unavailable") }
                ?: throw IOException("사진 위치 정보 응답을 확인할 수 없어요.")
            UploadedAsset(assetId, locationStatus)
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun createInspection(assetId: String, idempotencyKey: String, location: String?): JSONObject = withContext(Dispatchers.IO) {
        val connection = openConnection("/api/inspections", "POST")
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        connection.setRequestProperty("Idempotency-Key", idempotencyKey)
        connection.doOutput = true
        try {
            connection.outputStream.use { output ->
                val request = JSONObject().put("assetId", assetId)
                if (location != null) request.put("locationFallback", location)
                output.write(request.toString().toByteArray(Charsets.UTF_8))
            }
            readJsonResponse(connection)
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun getInspection(inspectionId: String): JSONObject = withContext(Dispatchers.IO) {
        if (inspectionId.isBlank()) throw IOException("분석 작업 ID가 없어요.")
        val connection = openConnection("/api/inspections/$inspectionId", "GET")
        try {
            readJsonResponse(connection)
        } finally {
            connection.disconnect()
        }
    }

    private fun openConnection(path: String, method: String): HttpURLConnection {
        val connection = URL("$baseUrl$path").openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = 10_000
        connection.readTimeout = 15_000
        connection.setRequestProperty("Accept", "application/json")
        return connection
    }

    private data class UploadedAsset(val assetId: String, val locationStatus: String)

    private fun readJsonResponse(connection: HttpURLConnection): JSONObject {
        val statusCode = connection.responseCode
        val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
        val body = stream?.use { input ->
            ByteArrayOutputStream().use { output ->
                input.copyTo(output)
                output.toString(Charsets.UTF_8.name())
            }
        }.orEmpty()
        if (statusCode !in 200..299) {
            val message = runCatching {
                JSONObject(body).getJSONObject("error").getString("message")
            }.getOrNull() ?: "서버와 통신하지 못했어요. 잠시 후 다시 시도해 주세요."
            throw IOException(message)
        }
        return runCatching { JSONObject(body) }
            .getOrElse { throw IOException("서버 응답을 읽을 수 없어요.") }
    }

    private fun JSONObject.toAnalysisResult(): AnalysisResult {
        val summary = optJSONObject("riskSummary")
            ?: throw IOException("분석 결과에 위험 요약이 없어요.")
        val score = (summary.opt("score") as? Number)?.toInt()
            ?: throw IOException("분석 결과에 점수가 없어요.")
        if (score !in 0..100) throw IOException("분석 점수 범위를 확인해 주세요.")
        val defaultRisk = summary.optString("level", "caution").toClueRisk()
        val factors = optJSONArray("riskFactors") ?: JSONArray()
        val clues = buildList {
            for (index in 0 until factors.length()) {
                val factor = factors.optJSONObject(index) ?: continue
                val id = factor.optString("id").ifBlank { "risk-$index" }
                val type = factor.optString("type")
                add(
                    AnalysisClue(
                        id = id,
                        title = factor.optString("title").ifBlank { "위치 노출 단서" },
                        description = factor.optString("description"),
                        kind = type.toClueKind(),
                        risk = factor.optString("risk", factor.optString("level")).toClueRisk(defaultRisk),
                        region = factor.toNormalizedRegion(),
                    ),
                )
            }
        }
        return AnalysisResult(score = score, clues = clues, isMock = optBoolean("mock", false))
    }

    private fun JSONObject.toNormalizedRegion(): NormalizedRegion? {
        if (optString("coordinateSpace") != "analysis-normalized") return null
        val bbox = optJSONArray("bbox") ?: return null
        if (bbox.length() != 4) return null
        val values = (0 until 4).map { index -> (bbox.opt(index) as? Number)?.toFloat() ?: return null }
        return runCatching { NormalizedRegion(values[0], values[1], values[2], values[3]) }.getOrNull()
    }

    private fun String.toClueKind(): ClueKind = when (lowercase()) {
        "sign_text", "text", "ocr" -> ClueKind.TEXT
        "building", "building_view", "sign", "logo" -> ClueKind.BUILDING
        "metadata", "gps", "exif" -> ClueKind.METADATA
        else -> ClueKind.TEXT
    }

    private fun String.toClueRisk(fallback: ClueRisk = ClueRisk.CAUTION): ClueRisk = when (lowercase()) {
        "high", "높음", "critical" -> ClueRisk.HIGH
        "low", "낮음" -> ClueRisk.LOW
        "caution", "medium", "주의" -> ClueRisk.CAUTION
        else -> fallback
    }
}
