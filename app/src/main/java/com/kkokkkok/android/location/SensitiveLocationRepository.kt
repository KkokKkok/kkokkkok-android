package com.kkokkkok.android.location

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
    val riskScore: Int = 0,
    val photoScans: Int = 0,
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
