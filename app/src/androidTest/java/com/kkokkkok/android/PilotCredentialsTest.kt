package com.kkokkkok.android

import androidx.test.platform.app.InstrumentationRegistry
import com.kkokkkok.android.pilot.PilotCredentials
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class PilotCredentialsTest {
    @Test fun credentialIsEncryptedAndRestoredWithoutReEnrollment() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        PilotCredentials.initialize(context)
        val synthetic = "kkp_" + "A".repeat(43)
        try {
            PilotCredentials.save(JSONObject().put("installationId", "11111111-1111-4111-8111-111111111111").put("token", synthetic))
            PilotCredentials.initialize(context)
            assertEquals("11111111-1111-4111-8111-111111111111", PilotCredentials.installationId())
            val files = java.io.File(context.applicationInfo.dataDir, "shared_prefs").listFiles().orEmpty()
                .filter { it.name.startsWith("pilot_credentials_") }
            assertTrue(files.isNotEmpty())
            assertFalse(files.any { it.readText().contains(synthetic) })
            val connection = URL(BuildConfig.BACKEND_BASE_URL + "/api/assets").openConnection() as HttpURLConnection
            PilotCredentials.authorize(connection)
            assertEquals("Bearer $synthetic", connection.getRequestProperty("Authorization"))
            assertFalse(connection.instanceFollowRedirects)
        } finally { PilotCredentials.invalidate() }
    }
    @Test fun credentialIsNeverSentToAnotherServer() {
        PilotCredentials.initialize(InstrumentationRegistry.getInstrumentation().targetContext)
        try {
            PilotCredentials.save(JSONObject().put("installationId", "11111111-1111-4111-8111-111111111111").put("token", "kkp_" + "B".repeat(43)))
            val other = URL("https://example.invalid/api/assets").openConnection() as HttpURLConnection
            PilotCredentials.authorize(other)
            assertNull(other.getRequestProperty("Authorization"))
            assertNull(other.getRequestProperty("X-Installation-Id"))
        } finally { PilotCredentials.invalidate() }
    }
}
