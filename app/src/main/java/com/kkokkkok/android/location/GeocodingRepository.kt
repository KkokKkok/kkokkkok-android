package com.kkokkkok.android.location

import com.kkokkkok.android.pilot.PilotCredentials
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** WGS84 point. Always latitude first; the backend already converted Kakao's x/y order. */
data class GeoPoint(val latitude: Double, val longitude: Double) {
    init { require(latitude in -90.0..90.0 && longitude in -180.0..180.0) { "Invalid coordinate" } }
    companion object {
        fun parse(latitude: String, longitude: String): GeoPoint? {
            val lat = latitude.toDoubleOrNull() ?: return null
            val lng = longitude.toDoubleOrNull() ?: return null
            return runCatching { GeoPoint(lat, lng) }.getOrNull()
        }
    }
}

data class PlaceSearchResult(val name: String, val address: String?, val point: GeoPoint, val isAddress: Boolean)

/** User-entered address/place search for the map UI (backend → Kakao Local). Never infers a photo's location. */
interface GeocodingRepository {
    suspend fun search(query: String): List<PlaceSearchResult>
    suspend fun reverse(point: GeoPoint): String?
}

class BackendGeocodingRepository(baseUrl: String) : GeocodingRepository {
    private val baseUrl = baseUrl.trimEnd('/')

    override suspend fun search(query: String): List<PlaceSearchResult> = withContext(Dispatchers.IO) {
        val json = get("/api/geocoding/search?query=" + URLEncoder.encode(query.trim(), "UTF-8"))
        parseSearch(json)
    }

    override suspend fun reverse(point: GeoPoint): String? = withContext(Dispatchers.IO) {
        val json = get("/api/geocoding/reverse?latitude=${point.latitude}&longitude=${point.longitude}")
        json.optString("roadAddress").takeIf { it.isNotBlank() && it != "null" }
            ?: json.optString("address").takeIf { it.isNotBlank() && it != "null" }
    }

    private fun get(path: String): JSONObject {
        val connection = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 15_000
            PilotCredentials.authorize(this)
            setRequestProperty("Accept", "application/json")
        }
        try {
            val code = PilotCredentials.responseCode(connection)
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw IOException(
                when (code) {
                    429 -> "검색 요청이 많아요. 잠시 후 다시 시도해 주세요."
                    503 -> "주소 검색이 설정되지 않았어요."
                    else -> "주소를 검색하지 못했어요. 다시 시도해 주세요."
                },
            )
            return JSONObject(text)
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        internal fun parseSearch(json: JSONObject): List<PlaceSearchResult> {
            val array = json.optJSONArray("results") ?: return emptyList()
            return (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                val point = runCatching { GeoPoint(item.getDouble("latitude"), item.getDouble("longitude")) }.getOrNull()
                    ?: return@mapNotNull null
                PlaceSearchResult(
                    name = item.optString("name").ifBlank { return@mapNotNull null },
                    address = item.optString("address").takeIf { it.isNotBlank() && it != "null" },
                    point = point,
                    isAddress = item.optString("type") == "address",
                )
            }
        }
    }
}
