package com.kkokkkok.android.profile

enum class ProfilePlaceKind { HOME, SCHOOL, WORK, OTHER }

data class ProfilePlace(val name: String, val kind: ProfilePlaceKind)

/** UI-only profile values for the preview. This shape is not an API contract. */
data class ProfileOverview(
    val displayName: String,
    val photoChecks: Int,
    val protectionEdits: Int,
    val snsChecks: Int,
    val sensitivePlaces: List<ProfilePlace>,
    val autoDeleteOriginals: Boolean,
    val retentionDays: Int,
    val isMock: Boolean = true,
    val sharedLocalData: Boolean = false,
)

interface ProfileRepository {
    suspend fun deleteAll() { throw UnsupportedOperationException("모의 화면에서는 삭제할 수 없어요.") }
    suspend fun getOverview(): ProfileOverview
}

/** Synthetic account preview; it contains no real user profile or settings. */
class MockProfileRepository : ProfileRepository {
    override suspend fun getOverview() = ProfileOverview(
        displayName = "테스터",
        photoChecks = 12,
        protectionEdits = 5,
        snsChecks = 1,
        sensitivePlaces = listOf(
            ProfilePlace("집 주변", ProfilePlaceKind.HOME),
            ProfilePlace("학교", ProfilePlaceKind.SCHOOL),
        ),
        autoDeleteOriginals = false,
        retentionDays = 0,
    )
}

/** Reads account-independent statistics; credentials remain in the shared backend session. */
class BackendProfileRepository(context: android.content.Context, baseUrl: String) : ProfileRepository {
    private val appContext = context.applicationContext
    private val identity = com.kkokkkok.android.network.InstallationIdentity(context.applicationContext)
    private val baseUrl = baseUrl.trimEnd('/')
    override suspend fun getOverview(): ProfileOverview = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val connection = java.net.URL("$baseUrl/api/profile").openConnection() as java.net.HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("X-Installation-Id", identity.get())
            com.kkokkkok.android.pilot.PilotCredentials.authorize(connection)
            val code = com.kkokkkok.android.pilot.PilotCredentials.responseCode(connection)
            if (code !in 200..299) throw java.io.IOException("마이 페이지를 불러오지 못했어요.")
            val json = org.json.JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val places = json.getJSONArray("sensitivePlaces")
            ProfileOverview(json.getString("displayName"), json.getInt("photoChecks"),
                json.getInt("protectionEdits"), json.getInt("cumulativeChecks"),
                (0 until places.length()).map { index ->
                    val place = places.getJSONObject(index)
                    ProfilePlace(place.getString("name"), when(place.getString("category")) {
                        "home" -> ProfilePlaceKind.HOME
                        "school" -> ProfilePlaceKind.SCHOOL
                        "work" -> ProfilePlaceKind.WORK
                        else -> ProfilePlaceKind.OTHER
                    })
                }, json.getJSONObject("storagePolicy").getBoolean("autoDeleteOriginals"), 0, false, json.optString("dataScope") == "local_shared")
        } finally { connection.disconnect() }
    }
    override suspend fun deleteAll() = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val connection = java.net.URL("$baseUrl/api/profile/data").openConnection() as java.net.HttpURLConnection
        try {
            connection.requestMethod = "DELETE"
            connection.connectTimeout = 10_000
            connection.readTimeout = 120_000
            connection.setRequestProperty("X-Installation-Id", identity.get())
            com.kkokkkok.android.pilot.PilotCredentials.authorize(connection)
            val code = com.kkokkkok.android.pilot.PilotCredentials.responseCode(connection)
            if (code !in 200..299) {
                val error = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                val message = runCatching { org.json.JSONObject(error).getJSONObject("error").getString("message") }.getOrNull()
                throw java.io.IOException(message ?: "삭제를 완료하지 못했어요. 다시 시도해 주세요.")
            }
        } finally { connection.disconnect() }
        if (!appContext.cacheDir.deleteRecursively()) throw java.io.IOException("서버 자료는 삭제했지만 기기 임시 파일을 정리하지 못했어요. 다시 시도해 주세요.")
        appContext.cacheDir.mkdirs()
        if (!appContext.getSharedPreferences("sensitive_place_coordinates", android.content.Context.MODE_PRIVATE).edit().clear().commit())
            throw java.io.IOException("서버 자료는 삭제했지만 기기 위치 정보를 정리하지 못했어요. 다시 시도해 주세요.")
    }

}

class ProfileViewModelFactory(private val repository: ProfileRepository) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ProfileViewModel::class.java))
        return ProfileViewModel(repository) as T
    }
}
