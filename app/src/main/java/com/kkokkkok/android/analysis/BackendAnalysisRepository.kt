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
    private val pollIntervalMillis: Long = 1_000,
    private val maxPollCount: Int = 300,
) : AnalysisRepository {
    private val baseUrl = baseUrl.trimEnd('/')
    private val uploadMutex = Mutex()
    private val cachedUploads = mutableMapOf<Uri, UploadedAsset>()

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

        emit(parseInspectionState(result))
    }

    override fun analyzeBatch(photos: List<AnalysisBatchPhoto>): Flow<List<AnalysisState>> = flow {
        require(photos.isNotEmpty() && photos.size <= 20) { "한 번에 1~20장의 사진을 분석할 수 있어요." }
        val states = MutableList<AnalysisState>(photos.size) { AnalysisState.Running(AnalysisStep.METADATA, 5) }
        emit(states.toList())
        val assets = photos.mapIndexed { index, photo ->
            states[index] = AnalysisState.Running(AnalysisStep.METADATA, 15)
            emit(states.toList())
            getOrUploadAsset(photo.uri)
        }
        var batch = createInspectionBatch(photos.zip(assets))
        var polls = 0
        while (batchItems(batch).any { it.optString("status") in setOf("queued", "processing") }) {
            if (polls >= maxPollCount) throw IOException("분석 결과를 기다리는 시간이 초과됐어요.")
            delay(pollIntervalMillis)
            batch = getInspectionBatch(batch.optString("batchId"))
            polls += 1
            val items = batchItems(batch)
            items.forEachIndexed { index, item ->
                states[index] = when (item.optString("status")) {
                    "completed" -> AnalysisState.Running(AnalysisStep.BUILDINGS, 90)
                    "failed" -> AnalysisState.Failed
                    else -> AnalysisState.Running(if (polls < 3) AnalysisStep.SIGNS else AnalysisStep.BUILDINGS,
                        (45 + polls * 2).coerceAtMost(95))
                }
            }
            emit(states.toList())
        }
        batchItems(batch).forEachIndexed { index, item ->
            if (item.optString("status") in setOf("completed", "failed")) {
                states[index] = try {
                    val inspection = getInspection(item.optString("inspectionId"))
                    parseInspectionState(inspection)
                } catch (_: Exception) {
                    AnalysisState.Failed
                }
            } else states[index] = AnalysisState.Failed
            emit(states.toList())
        }
    }

    override suspend fun hasExifGps(photo: Uri): Boolean = getOrUploadAsset(photo).locationStatus == "available"

    private suspend fun getOrUploadAsset(uri: Uri): UploadedAsset = uploadMutex.withLock {
        cachedUploads[uri]?.let { return it }
        uploadAsset(uri).also { cachedUploads[uri] = it }
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

    private suspend fun createInspectionBatch(photos: List<Pair<AnalysisBatchPhoto, UploadedAsset>>): JSONObject = withContext(Dispatchers.IO) {
        val connection = openConnection("/api/inspection-batches", "POST")
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        connection.setRequestProperty("Idempotency-Key", UUID.randomUUID().toString())
        connection.doOutput = true
        try {
            val items = JSONArray()
            photos.forEach { (photo, asset) ->
                val item = JSONObject().put("assetId", asset.assetId)
                if (photo.location.isNotBlank()) item.put("locationFallback", photo.location)
                items.put(item)
            }
            connection.outputStream.use { it.write(JSONObject().put("items", items).toString().toByteArray(Charsets.UTF_8)) }
            readJsonResponse(connection)
        } finally { connection.disconnect() }
    }

    private suspend fun getInspectionBatch(batchId: String): JSONObject = withContext(Dispatchers.IO) {
        if (batchId.isBlank()) throw IOException("묶음 분석 ID가 없어요.")
        val connection = openConnection("/api/inspection-batches/$batchId", "GET")
        try { readJsonResponse(connection) } finally { connection.disconnect() }
    }

    private fun batchItems(batch: JSONObject): List<JSONObject> {
        val items = batch.optJSONArray("items") ?: throw IOException("묶음 분석 응답을 확인할 수 없어요.")
        return (0 until items.length()).map { items.optJSONObject(it) ?: JSONObject().put("status", "failed") }
    }

    private fun openConnection(path: String, method: String): HttpURLConnection {
        val connection = URL("$baseUrl$path").openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = 10_000
        connection.readTimeout = 15_000
        connection.setRequestProperty("Accept", "application/json")
        com.kkokkkok.android.pilot.PilotCredentials.authorize(connection)
        return connection
    }

    private data class UploadedAsset(val assetId: String, val locationStatus: String)

    private fun readJsonResponse(connection: HttpURLConnection): JSONObject {
        val statusCode = com.kkokkkok.android.pilot.PilotCredentials.responseCode(connection)
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

}

/** Retains usable clues when the pipeline reports a failed run with partial results. */
internal fun parseInspectionState(response: JSONObject): AnalysisState = when (response.optString("status")) {
    "completed" -> AnalysisState.Completed(parseAnalysisResult(response))
    "failed" -> {
        val partial = parseAnalysisResult(response)
        if (partial.clues.isEmpty()) AnalysisState.Failed
        else AnalysisState.Completed(partial.copy(isPartial = true))
    }
    else -> throw IOException("분석 서버 응답 상태를 확인할 수 없어요.")
}

/** Maps the current provisional backend result shape without inventing a score or clue level. */
internal fun parseAnalysisResult(response: JSONObject): AnalysisResult {
    val summary = response.optJSONObject("riskSummary")
        ?: if (response.optString("status") == "failed") JSONObject() else throw IOException("분석 결과에 위험 요약이 없어요.")
    val score = (summary.opt("score") as? Number)?.toInt()
    if (score != null && score !in 0..100) throw IOException("분석 점수 범위를 확인해 주세요.")
    val factors = response.optJSONArray("riskFactors") ?: JSONArray()
    val clues = buildList {
        val seenIds = mutableSetOf<String>()
        for (index in 0 until factors.length()) {
            val factor = factors.optJSONObject(index) ?: continue
            val id = factor.optString("id").ifBlank { "risk-$index" }
            seenIds += id
            val type = factor.optString("type")
            val kind = type.toClueKind()
            val suppliedTitle = factor.optString("title").trim()
            val risk = listOf("humanRisk", "modelRisk", "risk", "level")
                .asSequence()
                .mapNotNull { key -> (factor.opt(key) as? String)?.toClueRiskOrNull() }
                .firstOrNull()
            val description = buildList {
                factor.optString("description").takeIf(String::isNotBlank)?.let(::add)
                factor.optString("detail").takeIf(String::isNotBlank)?.let { add("영역: $it") }
                factor.optString("modelReason").takeIf(String::isNotBlank)?.let { add("판단 근거: $it") }
            }.joinToString("\n")
            add(
                AnalysisClue(
                    id = id,
                    title = suppliedTitle.takeIf { it.isNotBlank() && !it.equals(type, ignoreCase = true) }
                        ?: kind.defaultDisplayTitle(),
                    description = description,
                    kind = kind,
                    risk = risk,
                    region = factor.toNormalizedRegion(),
                ),
            )
        }
        val observations = response.optJSONArray("visualObservations") ?: JSONArray()
        for (index in 0 until observations.length()) {
            val observation = observations.optJSONObject(index) ?: continue
            val id = observation.optString("id").ifBlank { "observation-$index" }
            if (!seenIds.add(id)) {
                val existingIndex = indexOfFirst { it.id == id }
                val observedRegion = observation.toNormalizedRegion()
                if (existingIndex >= 0 && get(existingIndex).region == null && observedRegion != null) {
                    set(existingIndex, get(existingIndex).copy(region = observedRegion))
                }
                continue
            }
            val source = observation.optString("source")
            val confidence = (observation.opt("confidence") as? Number)?.toFloat()
            val description = buildList {
                observation.optString("regionDescription").takeIf(String::isNotBlank)?.let { add("영역: $it") }
                observation.optString("modelReason").takeIf(String::isNotBlank)?.let(::add)
                when (source) {
                    "paddleocr" -> confidence?.let { add("문자 인식 신뢰도: ${(it * 100).toInt()}% (위험도와는 다른 값)") }
                    "grounding-dino" -> confidence?.let { add("객체 탐지 신뢰도: ${(it * 100).toInt()}% (위험도와는 다른 값)") }
                }
            }.joinToString("\n")
            add(
                AnalysisClue(
                    id = id,
                    title = observation.optString("name").ifBlank { "사진 속 관찰 항목" },
                    description = description,
                    kind = (observation.optString("category") + " " + source).toClueKind(),
                    risk = null,
                    region = observation.toNormalizedRegion(),
                ),
            )
        }
    }
    val stages = response.optJSONObject("pipelineStages")
    val isPartial = listOf("ocr", "objectDetection").any { stage ->
        stages?.optString(stage) in setOf("failed", "unavailable")
    }
    return AnalysisResult(
        score = score,
        clues = clues,
        isMock = response.optBoolean("mock", false),
        isPartial = isPartial,
        inspectionId = response.optString("inspectionId").takeIf(String::isNotBlank),
    )
}

private fun String.toClueRiskOrNull(): ClueRisk? = when (lowercase()) {
    "high", "높음", "critical" -> ClueRisk.HIGH
    "low", "낮음" -> ClueRisk.LOW
    "caution", "medium", "주의" -> ClueRisk.CAUTION
    else -> null
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
    "object", "object paddleocr", "object grounding-dino", "grounding-dino" -> ClueKind.OBJECT
    "metadata", "gps", "exif" -> ClueKind.METADATA
    else -> ClueKind.TEXT
}

private fun ClueKind.defaultDisplayTitle(): String = when (this) {
    ClueKind.TEXT -> "사진 속 글자"
    ClueKind.BUILDING -> "주변 건물·풍경"
    ClueKind.OBJECT -> "주변 물체"
    ClueKind.METADATA -> "사진 위치 정보"
}
