package com.kkokkkok.android.snscheck

import android.content.Context
import android.net.Uri
import androidx.annotation.DrawableRes
import com.kkokkkok.android.network.InstallationIdentity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class SnsPlaceOption(
    val id: String,
    val name: String,
    val detail: String,
    val metadata: String,
    val kind: SnsPlaceKind,
)

enum class SnsPlaceKind { HOME, SCHOOL, OTHER }
enum class SnsPostPeriod { RECENT, THIS_YEAR }

data class SnsPostPreview(
    val id: String,
    @param:DrawableRes val thumbnail: Int = android.R.drawable.ic_menu_gallery,
    val period: SnsPostPeriod = SnsPostPeriod.THIS_YEAR,
    val initiallySelected: Boolean = false,
    val imageUri: Uri? = null,
    val title: String = "사진",
    val locationStatus: String = "unavailable",
    val createdAt: String = "",
)

data class SnsExposureCue(val id: String, val title: String, val postCount: Int, val detail: String = "")
data class CumulativeGroupSummary(
    val id: String,
    val name: String,
    val photoCount: Int,
    val independentPhotoCount: Int,
    val visitDays: Int,
    val distanceBand: String,
    val evidence: List<String>,
)

data class SnsExposureResult(
    val sampleScore: Int?,
    val sampleRiskLabel: String,
    val selectedPostCount: Int,
    val cues: List<SnsExposureCue>,
    val posts: List<SnsPostPreview>,
    val isMock: Boolean,
    val algorithmVersion: String,
    val scoreStatus: String,
    val notice: String,
    val groups: List<CumulativeGroupSummary> = emptyList(),
    val excluded: List<String> = emptyList(),
)

interface SnsCumulativeRepository {
    suspend fun getPlaceOptions(): List<SnsPlaceOption>
    suspend fun getPostPreviews(placeId: String): List<SnsPostPreview>
    suspend fun getLinkedAssetIds(placeId: String): Set<String>
    suspend fun getExposureResult(
        placeId: String,
        selectedPostIds: Set<String>,
        manualLocations: Map<String, Pair<String, String>>,
    ): SnsExposureResult
}

/** Uses saved photos, their latest single-analysis results, and the provisional cumulative API. */
class BackendSnsCumulativeRepository(
    context: Context,
    baseUrl: String,
) : SnsCumulativeRepository {
    private val appContext = context.applicationContext
    private val baseUrl = baseUrl.trimEnd('/')
    private val identity = InstallationIdentity(appContext)
    private val previewCache = File(appContext.cacheDir, "cumulative-previews")

    override suspend fun getPlaceOptions(): List<SnsPlaceOption> = withContext(Dispatchers.IO) {
        val places = getArray("/api/places")
        (0 until places.length()).mapNotNull { index ->
            val place = places.optJSONObject(index) ?: return@mapNotNull null
            val category = place.optString("category")
            val radius = place.optInt("radiusMeters", 300)
            SnsPlaceOption(
                id = place.optString("placeId"),
                name = place.optString("name"),
                detail = "보호 반경 ${if (radius == 1_000) "1km" else "${radius}m"}",
                metadata = "등록한 민감 장소",
                kind = when (category) {
                    "home" -> SnsPlaceKind.HOME
                    "school" -> SnsPlaceKind.SCHOOL
                    else -> SnsPlaceKind.OTHER
                },
            )
        }
    }

    override suspend fun getPostPreviews(placeId: String): List<SnsPostPreview> = withContext(Dispatchers.IO) {
        val assets = getArray("/api/assets")
        buildList {
            for (index in 0 until assets.length()) {
                val asset = assets.optJSONObject(index) ?: continue
                if (asset.optString("assetRole", "original") != "original") continue
                if (asset.optBoolean("mock", false)) continue
                val id = asset.optString("assetId").takeIf(String::isNotBlank) ?: continue
                val createdAt = asset.optString("createdAt")
                val image = runCatching { cachePhoto(id) }.getOrNull()
                val labels = asset.optJSONArray("labels").stringItems()
                add(SnsPostPreview(
                    id = id,
                period = if (runCatching { java.time.Instant.parse(createdAt).isAfter(java.time.Instant.now().minusSeconds(90L * 24 * 60 * 60)) }.getOrDefault(false)) SnsPostPeriod.RECENT else SnsPostPeriod.THIS_YEAR,
                    imageUri = image,
                    title = labels.firstOrNull() ?: "저장된 사진",
                    locationStatus = asset.optString("locationStatus", "unavailable"),
                    createdAt = createdAt,
                ))
            }
        }
    }

    override suspend fun getLinkedAssetIds(placeId: String): Set<String> = withContext(Dispatchers.IO) {
        val assets = getArray("/api/places/$placeId/assets")
        (0 until assets.length()).mapNotNull { assets.optJSONObject(it)?.optString("assetId")?.takeIf(String::isNotBlank) }.toSet()
    }

    override suspend fun getExposureResult(
        placeId: String,
        selectedPostIds: Set<String>,
        manualLocations: Map<String, Pair<String, String>>,
    ): SnsExposureResult = withContext(Dispatchers.IO) {
        if (selectedPostIds.isEmpty()) throw IOException("사진을 한 장 이상 선택해 주세요.")
        val allPhotos = getPostPreviews(placeId)
        val selected = allPhotos.filter { it.id in selectedPostIds }
        val missing = selected.filter { it.locationStatus != "available" && it.locationStatus != "manual" }
        for (photo in missing) {
            val pair = manualLocations[photo.id] ?: throw IOException("${photo.title}: 촬영 위도와 경도를 입력해 주세요.")
            val latitude = pair.first.toDoubleOrNull()?.takeIf { it in -90.0..90.0 }
            val longitude = pair.second.toDoubleOrNull()?.takeIf { it in -180.0..180.0 }
            if (latitude == null || longitude == null) throw IOException("${photo.title}: 위도·경도를 확인해 주세요.")
            requestJson("/api/assets/${photo.id}/capture-location", "PUT",
                JSONObject().put("latitude", latitude).put("longitude", longitude).put("source", "manual"), installationScoped = false)
        }
        selectedPostIds.forEach { id ->
            requestJson("/api/places/$placeId/assets", "POST", JSONObject().put("assetId", id), installationScoped = true)
        }
        val response = requestJson("/api/cumulative-runs", "POST",
            JSONObject().put("placeId", placeId).put("assetIds", JSONArray(selectedPostIds.toList())), installationScoped = true)
        val groups = response.optJSONArray("groups").objects().mapIndexed { index, group ->
            val evidence = group.optJSONArray("evidence").objects()
            CumulativeGroupSummary(
                id = group.optString("groupId", "group-$index"),
                name = group.optString("name", "장소 묶음"),
                photoCount = group.optInt("photoCount"),
                independentPhotoCount = group.optInt("independentPhotoCount"),
                visitDays = group.optInt("visitDays"),
                distanceBand = group.optString("placeDistanceBand"),
                evidence = evidence.map { it.optString("title") }.distinct(),
            )
        }
        val excludedRows = response.optJSONArray("excluded").objects()
        val excluded = excludedRows.map { item ->
            val title = selected.firstOrNull { it.id == item.optString("assetId") }?.title ?: "사진"
            "$title · ${item.optString("reason")}"
        }
        val excludedIds = excludedRows.mapTo(mutableSetOf()) { it.optString("assetId") }
        val cues = groups.filter { it.evidence.isNotEmpty() }.map {
            SnsExposureCue(it.id, it.name, it.evidence.size, "${it.visitDays}일 방문 · ${it.evidence.joinToString(" · ")}")
        }
        SnsExposureResult(
            sampleScore = if (response.isNull("score")) null else response.optInt("score"),
            sampleRiskLabel = when (response.optString("riskLevel")) {
                "high" -> "높음"
                "caution" -> "주의"
                "low" -> "낮음"
                else -> "미산정"
            },
            selectedPostCount = selectedPostIds.size,
            cues = cues,
            posts = selected.filterNot { it.id in excludedIds },
            isMock = response.optBoolean("mock", false),
            algorithmVersion = response.optString("algorithmVersion"),
            scoreStatus = response.optString("scoreStatus"),
            notice = response.optString("scoreNotice"),
            groups = groups,
            excluded = excluded,
        )
    }

    private fun cachePhoto(assetId: String): Uri {
        previewCache.mkdirs()
        val target = File(previewCache, "$assetId.image")
        if (!target.isFile || target.length() == 0L) {
            val connection = openConnection("/api/assets/$assetId/content", installationScoped = false)
            try {
                if (connection.responseCode !in 200..299) throw IOException("사진을 불러오지 못했어요.")
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

    private fun getArray(path: String): JSONArray {
        val connection = openConnection(path, installationScoped = path.startsWith("/api/places"))
        try { return JSONArray(readResponse(connection)) } finally { connection.disconnect() }
    }

    private fun requestJson(path: String, method: String, body: JSONObject, installationScoped: Boolean): JSONObject {
        val connection = openConnection(path, installationScoped, method)
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        connection.doOutput = true
        try {
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            return JSONObject(readResponse(connection))
        } finally { connection.disconnect() }
    }

    private fun openConnection(path: String, installationScoped: Boolean, method: String = "GET") =
        (URL("$baseUrl$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 10_000
            readTimeout = if (path == "/api/cumulative-runs") 90_000 else 20_000
            setRequestProperty("Accept", "application/json, image/*")
            if (installationScoped) setRequestProperty("X-Installation-Id", identity.get())
        }

    private fun readResponse(connection: HttpURLConnection): String {
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        if (code !in 200..299) {
            val message = runCatching { JSONObject(body).getJSONObject("error").getString("message") }.getOrNull()
            throw IOException(message ?: "누적 분석 서버와 통신하지 못했어요.")
        }
        return body
    }

    private fun JSONArray?.objects(): List<JSONObject> = this?.let { array ->
        (0 until array.length()).mapNotNull { array.optJSONObject(it) }
    }.orEmpty()

    private fun JSONArray?.stringItems(): List<String> = this?.let { array ->
        (0 until array.length()).mapNotNull { array.optString(it).takeIf(String::isNotBlank) }
    }.orEmpty()
}
