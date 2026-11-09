package com.kkokkkok.android.protectionhistory

/** UI-only example record. This shape is not a backend API contract. */
data class ProtectionRecord(
    val id: String,
    val originalScore: Int?,
    val protectedScore: Int?,
    val date: String,
    val artwork: Int,
    val originalUri: android.net.Uri? = null,
    val editedUri: android.net.Uri? = null,
    val isMock: Boolean = true,
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

/** Saved edited assets, with scores only when an actual inspection provides them. */
class BackendProtectionHistoryRepository(context: android.content.Context, private val baseUrl: String) : ProtectionHistoryRepository {
    private val cache = java.io.File(context.cacheDir, "protection-history")
    override suspend fun getRecords(): List<ProtectionRecord> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val assets = org.json.JSONArray(read("/api/assets"))
        buildList {
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.optString("assetRole") != "edited") continue
                val id = asset.getString("assetId")
                val parent = asset.optString("parentAssetId")
                if (parent.isBlank() || parent == "null") continue
                add(ProtectionRecord(id, score(parent), score(id), asset.optString("createdAt").take(10), 0,
                    photo(parent), photo(id), asset.optBoolean("mock")))
            }
        }.reversed()
    }
    private fun score(id: String): Int? {
        val rows = org.json.JSONArray(read("/api/inspections?assetId=$id"))
        if (rows.length() == 0) return null
        val row = rows.getJSONObject(0)
        val result = org.json.JSONObject(read("/api/inspections/${row.getString("inspectionId")}"))
        if (result.optBoolean("mock")) return null
        val summary = result.optJSONObject("riskSummary") ?: return null
        return if (summary.isNull("score")) null else summary.optInt("score")
    }
    private fun photo(id: String): android.net.Uri {
        cache.mkdirs()
        val target = java.io.File(cache, "$id.image")
        if (!target.exists()) {
            val c = connection("/api/assets/$id/content")
            try {
                if (com.kkokkkok.android.pilot.PilotCredentials.responseCode(c) !in 200..299) throw java.io.IOException("사진을 불러오지 못했어요")
                val temp = java.io.File(cache, "$id.tmp")
                c.inputStream.use { input -> temp.outputStream().use(input::copyTo) }
                if (!temp.renameTo(target)) { temp.copyTo(target, true); temp.delete() }
            } finally { c.disconnect() }
        }
        return android.net.Uri.fromFile(target)
    }
    private fun connection(path: String) = (java.net.URL("${baseUrl.trimEnd('/')}$path").openConnection() as java.net.HttpURLConnection).apply {
        com.kkokkkok.android.pilot.PilotCredentials.authorize(this)
        connectTimeout = 10000; readTimeout = 20000
    }
    private fun read(path: String): String {
        val c = connection(path)
        try {
            if (com.kkokkkok.android.pilot.PilotCredentials.responseCode(c) !in 200..299) throw java.io.IOException("보호 기록을 불러오지 못했어요")
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally { c.disconnect() }
    }
}
