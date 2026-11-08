package com.kkokkkok.android.history

import android.content.Context
import android.net.Uri
import com.kkokkkok.android.analysis.AnalysisResult
import com.kkokkkok.android.analysis.AnalysisState
import com.kkokkkok.android.analysis.ClueRisk
import com.kkokkkok.android.analysis.parseInspectionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

enum class HistoryKind { PHOTO, SNS }
enum class HistoryRisk { HIGH, CAUTION, LOW, UNASSESSED }

/** UI model. These fields do not define the backend response contract. */
data class HistoryRecord(
    val id: String,
    val kind: HistoryKind,
    val group: String,
    val title: String,
    val detail: String,
    val clueCount: Int,
    val score: Int?,
    val protected: Boolean = false,
    val protectedScore: Int? = null,
    val risk: HistoryRisk?,
    val assetId: String? = null,
    val inspectionId: String? = null,
    val photoUri: Uri? = null,
    val result: AnalysisResult? = null,
    val inspectionStatus: String? = null,
    val isMock: Boolean = false,
    val sortAt: String? = null,
)

interface HistoryRepository {
    suspend fun getRecords(kind: HistoryKind): List<HistoryRecord>
    fun isMock(kind: HistoryKind): Boolean = false
}

/** Uses the backend's stored photo gallery and per-photo inspection history. */
class BackendHistoryRepository(
    context: Context,
    baseUrl: String,
) : HistoryRepository {
    private val appContext = context.applicationContext
    private val baseUrl = baseUrl.trimEnd('/')
    private val previewCache = File(appContext.cacheDir, "history-previews")
    private val mockRepository = MockHistoryRepository()

    init { require(this.baseUrl.startsWith("http://") || this.baseUrl.startsWith("https://")) }

    override fun isMock(kind: HistoryKind): Boolean = kind == HistoryKind.SNS

    override suspend fun getRecords(kind: HistoryKind): List<HistoryRecord> = withContext(Dispatchers.IO) {
        if (kind == HistoryKind.SNS) return@withContext mockRepository.getRecords(kind)

        val assets = getJsonArray("/api/assets")
        buildList {
            for (index in 0 until assets.length()) {
                val asset = assets.optJSONObject(index) ?: continue
                if (asset.optString("assetRole", "original") != "original") continue
                val assetId = asset.optString("assetId")
                if (assetId.isBlank()) continue
                val imageUri = runCatching { cachePhoto(assetId) }.getOrNull()
                val labels = asset.optJSONArray("labels").toStringList()
                val title = labels.firstOrNull() ?: "사진 검사"
                val history = getJsonArray("/api/inspections?assetId=$assetId")
                if (history.length() == 0) {
                    add(
                        HistoryRecord(
                            id = "asset:$assetId", kind = HistoryKind.PHOTO,
                            group = dateGroup(asset.optString("createdAt")), title = title,
                            detail = "아직 검사하지 않았어요", clueCount = 0, score = null,
                            risk = HistoryRisk.UNASSESSED, assetId = assetId, photoUri = imageUri,
                            inspectionStatus = "not_started", sortAt = asset.optString("createdAt"),
                        ),
                    )
                    continue
                }
                for (historyIndex in 0 until history.length()) {
                    val item = history.optJSONObject(historyIndex) ?: continue
                    val inspectionId = item.optString("inspectionId").takeIf(String::isNotBlank)
                    val status = item.optString("status", "unknown")
                    val detailResponse = inspectionId?.let {
                        if (status == "completed" || status == "failed") runCatching { getJsonObject("/api/inspections/$it") }.getOrNull()
                        else null
                    }
                    val result = detailResponse?.let { response ->
                        runCatching { parseInspectionState(response) as? AnalysisState.Completed }
                            .getOrNull()?.result
                    }
                    val score = result?.score
                    val visibleClueCount = result?.clues?.count {
                        it.risk == ClueRisk.HIGH || it.risk == ClueRisk.CAUTION
                    } ?: 0
                    add(
                        HistoryRecord(
                            id = inspectionId ?: "asset:$assetId:$historyIndex",
                            kind = HistoryKind.PHOTO,
                            group = dateGroup(item.optString("requestedAt").ifBlank { asset.optString("createdAt") }),
                            title = title,
                            detail = when (status) {
                                "queued", "processing", "submitting" -> "검사 중"
                                "failed" -> "검사에 실패했어요"
                                else -> formatDate(item.optString("requestedAt"))
                            },
                            clueCount = visibleClueCount,
                            score = score,
                            risk = score.toHistoryRisk(),
                            assetId = assetId,
                            inspectionId = inspectionId,
                            photoUri = imageUri,
                            result = result,
                            inspectionStatus = status,
                            isMock = result?.isMock == true,
                            sortAt = item.optString("requestedAt").ifBlank { asset.optString("createdAt") },
                        ),
                    )
                }
            }
        }.sortedByDescending { it.sortAt.orEmpty() }
    }

    private fun cachePhoto(assetId: String): Uri {
        previewCache.mkdirs()
        val target = File(previewCache, "$assetId.image")
        if (!target.isFile || target.length() == 0L) {
            val connection = openConnection("/api/assets/$assetId/content")
            try {
                val code = connection.responseCode
                if (code !in 200..299) throw IOException("사진을 불러오지 못했어요.")
                val temporary = File(previewCache, "$assetId.tmp")
                connection.inputStream.use { input -> temporary.outputStream().use(input::copyTo) }
                if (!temporary.renameTo(target)) {
                    temporary.copyTo(target, overwrite = true)
                    temporary.delete()
                }
            } finally { connection.disconnect() }
        }
        return Uri.fromFile(target)
    }

    private fun getJsonArray(path: String): JSONArray {
        val connection = openConnection(path)
        try {
            val body = readResponse(connection)
            return JSONArray(body)
        } finally { connection.disconnect() }
    }

    private fun getJsonObject(path: String): JSONObject {
        val connection = openConnection(path)
        try { return JSONObject(readResponse(connection)) }
        finally { connection.disconnect() }
    }

    private fun openConnection(path: String): HttpURLConnection =
        (URL("$baseUrl$path").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 20_000
            setRequestProperty("Accept", "application/json, image/*")
        }

    private fun readResponse(connection: HttpURLConnection): String {
        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        if (status !in 200..299) throw IOException("기록을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.")
        return body
    }

    private fun JSONArray?.toStringList(): List<String> = this?.let { array ->
        (0 until array.length()).mapNotNull { array.optString(it).takeIf(String::isNotBlank) }
    }.orEmpty()

    private fun String.toHistoryInstant(): Instant? = runCatching { Instant.parse(this) }.getOrNull()

    private fun dateGroup(value: String): String {
        val date = value.toHistoryInstant() ?: return "날짜 미상"
        val today = Instant.now().atZone(ZoneId.systemDefault()).toLocalDate()
        val localDate = date.atZone(ZoneId.systemDefault()).toLocalDate()
        return when {
            localDate == today -> "오늘"
            ChronoUnit.DAYS.between(localDate, today) < 7 -> "이번 주"
            else -> localDate.format(DateTimeFormatter.ofPattern("yyyy년 M월"))
        }
    }

    private fun formatDate(value: String): String {
        val date = value.toHistoryInstant() ?: return "검사 완료"
        return DateTimeFormatter.ofPattern("M월 d일 HH:mm").withZone(ZoneId.systemDefault()).format(date)
    }

    private fun Int?.toHistoryRisk(): HistoryRisk = when {
        this == null -> HistoryRisk.UNASSESSED
        this >= 70 -> HistoryRisk.HIGH
        this >= 40 -> HistoryRisk.CAUTION
        else -> HistoryRisk.LOW
    }
}

/** Synthetic records for previews and the SNS tab until that feature is connected. */
class MockHistoryRepository : HistoryRepository {
    override fun isMock(kind: HistoryKind): Boolean = true

    override suspend fun getRecords(kind: HistoryKind): List<HistoryRecord> = when (kind) {
        HistoryKind.PHOTO -> photoRecords
        HistoryKind.SNS -> snsRecords
    }

    private val photoRecords = listOf(
        HistoryRecord("photo-cafe", HistoryKind.PHOTO, "오늘", "카페 창가 사진", "오후 3:12", 3, 72, true, 18, HistoryRisk.LOW, isMock = true),
        HistoryRecord("photo-selfie", HistoryKind.PHOTO, "오늘", "등굣길 셀피", "오전 8:40", 2, 58, risk = HistoryRisk.CAUTION, isMock = true),
        HistoryRecord("photo-walk", HistoryKind.PHOTO, "이번 주", "동네 산책 사진", "10월 11일", 4, 81, risk = HistoryRisk.HIGH, isMock = true),
        HistoryRecord("photo-brunch", HistoryKind.PHOTO, "이번 주", "친구랑 브런치", "10월 10일", 1, 22, risk = HistoryRisk.LOW, isMock = true),
    )

    private val snsRecords = listOf(
        HistoryRecord("sns-cafe", HistoryKind.SNS, "이번 주", "주말 카페 게시물", "10월 13일", 3, 72, risk = HistoryRisk.HIGH, isMock = true),
        HistoryRecord("sns-school", HistoryKind.SNS, "이번 주", "학교 주변 게시물", "10월 2일", 2, 58, risk = HistoryRisk.CAUTION, isMock = true),
    )
}
