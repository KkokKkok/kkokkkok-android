package com.kkokkkok.android.protectionhistory

/** UI-only example record. This shape is not a backend API contract. */
data class ProtectionRecord(
    val id: String,
    val originalScore: Int,
    val protectedScore: Int,
    val date: String,
    val artwork: Int,
)

interface ProtectionHistoryRepository {
    suspend fun getRecords(): List<ProtectionRecord>
}

/** Synthetic examples only; no real images or saved edits are read. */
class MockProtectionHistoryRepository : ProtectionHistoryRepository {
    override suspend fun getRecords(): List<ProtectionRecord> = listOf(
        ProtectionRecord("edit-cafe", 72, 18, "오늘", 0),
        ProtectionRecord("edit-selfie", 81, 25, "10월 11일", 1),
        ProtectionRecord("edit-building", 64, 20, "10월 8일", 2),
        ProtectionRecord("edit-brunch", 55, 12, "10월 5일", 3),
    )
}
