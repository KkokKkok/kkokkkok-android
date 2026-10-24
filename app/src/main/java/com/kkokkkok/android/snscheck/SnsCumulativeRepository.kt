package com.kkokkkok.android.snscheck

import androidx.annotation.DrawableRes
import com.kkokkkok.android.R

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
    @param:DrawableRes val thumbnail: Int,
    val period: SnsPostPeriod,
    val initiallySelected: Boolean = true,
)

/** Temporary screen data only. These values do not describe a backend API. */
interface SnsCumulativeRepository {
    suspend fun getPlaceOptions(): List<SnsPlaceOption>
    suspend fun getPostPreviews(): List<SnsPostPreview>
}

/** Screen previews based on the read-only Figma mock screen. No user posts are loaded. */
class MockSnsCumulativeRepository : SnsCumulativeRepository {
    override suspend fun getPlaceOptions() = listOf(
        SnsPlaceOption("home", "우리 집 주변", "꼭꼭로 12 근처 · 반경 300m", "최근 검사 없음", SnsPlaceKind.HOME),
        SnsPlaceOption("school", "학교", "꼭꼭고등학교 · 반경 500m", "10월 2일 검사 · 주의", SnsPlaceKind.SCHOOL),
    )

    override suspend fun getPostPreviews() = listOf(
        SnsPostPreview("post-1", R.drawable.sns_post_1, SnsPostPeriod.RECENT),
        SnsPostPreview("post-2", R.drawable.sns_post_2, SnsPostPeriod.RECENT),
        SnsPostPreview("post-3", R.drawable.sns_post_3, SnsPostPeriod.RECENT),
        SnsPostPreview("post-4", R.drawable.sns_post_4, SnsPostPeriod.RECENT),
        SnsPostPreview("post-5", R.drawable.sns_post_1, SnsPostPeriod.THIS_YEAR),
        SnsPostPreview("post-6", R.drawable.sns_post_5, SnsPostPeriod.RECENT),
        SnsPostPreview("post-7", R.drawable.sns_post_2, SnsPostPeriod.THIS_YEAR),
        SnsPostPreview("post-8", R.drawable.sns_post_6, SnsPostPeriod.THIS_YEAR, initiallySelected = false),
        SnsPostPreview("post-9", R.drawable.sns_post_1, SnsPostPeriod.THIS_YEAR),
        SnsPostPreview("post-10", R.drawable.sns_post_3, SnsPostPeriod.RECENT),
        SnsPostPreview("post-11", R.drawable.sns_post_7, SnsPostPeriod.RECENT, initiallySelected = false),
        SnsPostPreview("post-12", R.drawable.sns_post_2, SnsPostPeriod.THIS_YEAR),
    )
}
