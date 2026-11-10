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
)

interface ProfileRepository {
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
                }, json.getJSONObject("storagePolicy").getBoolean("autoDeleteOriginals"), 0, false)
        } finally { connection.disconnect() }
    }
}

class ProfileViewModelFactory(private val repository: ProfileRepository) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ProfileViewModel::class.java))
        return ProfileViewModel(repository) as T
    }
}
