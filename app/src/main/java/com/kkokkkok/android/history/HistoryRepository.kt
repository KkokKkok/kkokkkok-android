package com.kkokkkok.android.history

enum class HistoryKind { PHOTO, SNS }
enum class HistoryRisk { HIGH, CAUTION, LOW }

/** UI-only history item. This shape is not a backend API contract. */
data class HistoryRecord(
    val id: String,
    val kind: HistoryKind,
    val group: String,
    val title: String,
    val detail: String,
    val clueCount: Int,
    val score: Int,
    val protected: Boolean = false,
    val protectedScore: Int? = null,
    val risk: HistoryRisk,
)

interface HistoryRepository {
    suspend fun getRecords(kind: HistoryKind): List<HistoryRecord>
}

/** Synthetic records for the UI preview. It contains no user history or photos. */
class MockHistoryRepository : HistoryRepository {
    override suspend fun getRecords(kind: HistoryKind): List<HistoryRecord> = when (kind) {
        HistoryKind.PHOTO -> photoRecords
        HistoryKind.SNS -> snsRecords
    }

    private val photoRecords = listOf(
        HistoryRecord("photo-cafe", HistoryKind.PHOTO, "오늘", "카페 창가 사진", "오후 3:12", 3, 72, true, 18, HistoryRisk.LOW),
        HistoryRecord("photo-selfie", HistoryKind.PHOTO, "오늘", "등굣길 셀피", "오전 8:40", 2, 58, risk = HistoryRisk.CAUTION),
        HistoryRecord("photo-walk", HistoryKind.PHOTO, "이번 주", "동네 산책 사진", "10월 11일", 4, 81, risk = HistoryRisk.HIGH),
        HistoryRecord("photo-brunch", HistoryKind.PHOTO, "이번 주", "친구랑 브런치", "10월 10일", 1, 22, risk = HistoryRisk.LOW),
    )

    private val snsRecords = listOf(
        HistoryRecord("sns-cafe", HistoryKind.SNS, "이번 주", "주말 카페 게시물", "10월 13일", 3, 72, risk = HistoryRisk.HIGH),
        HistoryRecord("sns-school", HistoryKind.SNS, "이번 주", "학교 주변 게시물", "10월 2일", 2, 58, risk = HistoryRisk.CAUTION),
    )
}
