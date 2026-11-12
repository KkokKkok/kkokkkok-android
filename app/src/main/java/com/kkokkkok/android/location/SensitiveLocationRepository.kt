package com.kkokkkok.android.location

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
/** Temporary UI model for the location preview. It is not a server API contract. */
data class SensitiveLocation(
    val id: String,
    val name: String,
    val type: LocationType,
    val address: String,
    val radiusMeters: Int,
    val risk: String? = null,
    val scanSummary: String = "아직 검사 전",
    val linkedPosts: Int = 0,
    val riskScore: Int? = null,
    val photoScans: Int = 0,
    val latitude: String = "",
    val longitude: String = "",
)

enum class LocationType(val label: String) {
    HOME("집 주변"), SCHOOL("학교"), WORK("직장"), FREQUENT("자주 가는 곳"),
}

interface SensitiveLocationRepository {
    suspend fun getLocations(): List<SensitiveLocation>
    suspend fun save(location: SensitiveLocation)
    suspend fun delete(id: String)
}

/** In-memory examples only. No real address, location, map, or SNS data is used. */
class MockSensitiveLocationRepository : SensitiveLocationRepository {
    private val locations = mutableListOf(
        SensitiveLocation("home", "우리 집 주변", LocationType.HOME, "꼭꼭로 12 근처", 300, "높음", "SNS 검사 10월 13일 · 게시물 10개", 10, 78, 3),
        SensitiveLocation("school", "학교", LocationType.SCHOOL, "꼭꼭고등학교", 500, "주의", "SNS 검사 10월 2일 · 게시물 4개", 4, 42, 1),
        SensitiveLocation("cafe", "단골 카페", LocationType.FREQUENT, "꼭꼭로 24", 100),
    )

    override suspend fun getLocations(): List<SensitiveLocation> = locations.toList()

    override suspend fun save(location: SensitiveLocation) {
        val index = locations.indexOfFirst { it.id == location.id }
        if (index >= 0) locations[index] = location else locations.add(0, location)
    }

    override suspend fun delete(id: String) {
        locations.removeAll { it.id == id }
    }
}

/** Keeps private place coordinates and address local to the app while the service returns only place labels. */
class BackendSensitiveLocationRepository(context: Context, baseUrl: String) : SensitiveLocationRepository {
    private val appContext = context.applicationContext
    private val baseUrl = baseUrl.trimEnd('/')
    private val identity = com.kkokkkok.android.network.InstallationIdentity(appContext)
    private val preferences = appContext.getSharedPreferences("sensitive_place_coordinates", Context.MODE_PRIVATE)

    override suspend fun getLocations(): List<SensitiveLocation> = withContext(Dispatchers.IO) {
        val response = request("/api/places", "GET")
        val array = JSONArray(response)
        (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val id = item.optString("placeId")
            val type = when (item.optString("category")) {
                "home" -> LocationType.HOME
                "work" -> LocationType.WORK
                "school" -> LocationType.SCHOOL
                else -> LocationType.FREQUENT
            }
            val linkedAssets = runCatching { JSONArray(request("/api/places/$id/assets", "GET")) }
                .getOrDefault(JSONArray())
            val checkedPhotos = (0 until linkedAssets.length()).count { assetIndex ->
                val assetId = linkedAssets.optJSONObject(assetIndex)?.optString("assetId") ?: return@count false
                runCatching { JSONArray(request("/api/inspections?assetId=$assetId", "GET")) }
                    .getOrDefault(JSONArray()).let { history ->
                        (0 until history.length()).any { history.optJSONObject(it)?.optString("status") == "completed" }
                    }
            }
            val runs = runCatching { JSONArray(request("/api/cumulative-runs/place/$id", "GET")) }
                .getOrDefault(JSONArray())
            val latestRun = runs.optJSONObject(0)
            SensitiveLocation(
                id = id,
                name = item.optString("name"),
                type = type,
                address = preferences.getString("$id.address", null) ?: "등록한 위치",
                radiusMeters = item.optInt("radiusMeters", 300),
                latitude = preferences.getString("$id.latitude", "").orEmpty(),
                longitude = preferences.getString("$id.longitude", "").orEmpty(),
                risk = when (latestRun?.optString("riskLevel")) {
                    "high" -> "높음"
                    "caution" -> "주의"
                    "low" -> "낮음"
                    else -> null
                },
                scanSummary = latestRun?.optString("createdAt")?.take(10)?.let { "최근 누적 검사 · $it" }
                    ?: "아직 누적 검사 전",
                linkedPosts = linkedAssets.length(),
                riskScore = latestRun?.takeUnless { it.isNull("score") }?.optInt("score"),
                photoScans = checkedPhotos,
            )
        }
    }

    override suspend fun save(location: SensitiveLocation) = withContext(Dispatchers.IO) {
        val latitude = location.latitude.toDoubleOrNull() ?: throw IOException("위치를 정해 주세요.")
        val longitude = location.longitude.toDoubleOrNull() ?: throw IOException("위치를 정해 주세요.")
        val body = JSONObject()
            .put("name", location.name)
            .put("category", location.type.toApiCategory())
            .put("latitude", latitude)
            .put("longitude", longitude)
            .put("radiusMeters", location.radiusMeters)
        val isNew = location.id.startsWith("new-")
        val response = JSONObject(request(
            if (isNew) "/api/places" else "/api/places/${location.id}",
            if (isNew) "POST" else "PUT",
            body,
        ))
        val id = response.optString("placeId").takeIf(String::isNotBlank) ?: location.id
        preferences.edit().putString("$id.latitude", location.latitude)
            .putString("$id.longitude", location.longitude).putString("$id.address", location.address).apply()
    }

    override suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        request("/api/places/$id", "DELETE")
        preferences.edit().remove("$id.latitude").remove("$id.longitude").remove("$id.address").apply()
    }

    private fun LocationType.toApiCategory() = when (this) {
        LocationType.HOME -> "home"
        LocationType.WORK -> "work"
        LocationType.SCHOOL -> "school"
        LocationType.FREQUENT -> "other"
    }

    private fun request(path: String, method: String, body: JSONObject? = null): String {
        val connection = (URL("$baseUrl$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 10_000
            readTimeout = 20_000
            com.kkokkkok.android.pilot.PilotCredentials.authorize(this)
            setRequestProperty("Accept", "application/json")
            setRequestProperty("X-Installation-Id", identity.get())
            if (body != null) {
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                doOutput = true
            }
        }
        try {
            if (body != null) connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = com.kkokkkok.android.pilot.PilotCredentials.responseCode(connection)
            if (code == HttpURLConnection.HTTP_NO_CONTENT) return ""
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                val message = runCatching { JSONObject(response).getJSONObject("error").getString("message") }.getOrNull()
                throw IOException(message ?: "장소 정보를 저장하지 못했어요.")
            }
            return response
        } finally { connection.disconnect() }
    }
}
