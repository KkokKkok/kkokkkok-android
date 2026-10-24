package com.kkokkkok.android.protection

import kotlinx.coroutines.delay

/** Temporary UI model; it is not an API request or a real image edit. */
data class ProtectionPreview(
    val beforeScore: Int = 72,
    val afterScore: Int = 18,
    val removedLabels: List<String> = listOf("창밖 간판과 건물", "GPS 위치 정보"),
    val remainingLabel: String = "매장 인테리어가 조금 보여요",
)

interface ProtectionEditRepository {
    suspend fun createMockPreview(): ProtectionPreview
}

class MockProtectionEditRepository : ProtectionEditRepository {
    override suspend fun createMockPreview(): ProtectionPreview {
        // Gives the button a brief, visible response without doing image or network work.
        delay(450)
        return ProtectionPreview()
    }
}
