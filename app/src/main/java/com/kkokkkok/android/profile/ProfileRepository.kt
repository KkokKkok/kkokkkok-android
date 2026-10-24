package com.kkokkkok.android.profile

enum class ProfilePlaceKind { HOME, SCHOOL }

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
        autoDeleteOriginals = true,
        retentionDays = 30,
    )
}
