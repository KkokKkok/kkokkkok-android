package com.kkokkkok.android.protection

import android.net.Uri
import com.kkokkkok.android.analysis.*

/** App editing input, obtained from an existing single-photo inspection. */
data class ProtectionSource(val assetId: String, val photo: Uri, val result: AnalysisResult)

fun AnalysisResult.hasEditableClues(): Boolean = !isMock && !inspectionId.isNullOrBlank() && clues.any {
    it.region != null && it.kind != ClueKind.METADATA && (it.risk == ClueRisk.HIGH || it.risk == ClueRisk.CAUTION)
}
