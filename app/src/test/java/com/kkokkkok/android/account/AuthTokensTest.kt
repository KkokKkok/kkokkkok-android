package com.kkokkkok.android.account

import java.time.Instant
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

/** Synthetic tokens only; verifies the backend auth response contract the app relies on. */
class AuthTokensTest {
    private val access = "kka_" + "A".repeat(43)
    private val refresh = "kkr_" + "B".repeat(43)

    private fun response(accessToken: String = access, refreshToken: String = refresh, name: String? = "합성 사용자") = JSONObject()
        .put("tokenType", "Bearer")
        .put("accessToken", accessToken).put("accessTokenExpiresAt", "2026-11-13T05:00:00Z")
        .put("refreshToken", refreshToken).put("refreshTokenExpiresAt", "2026-12-13T04:00:00Z")
        .put("user", JSONObject().put("userId", "22222222-2222-4222-8222-222222222222").apply { if (name != null) put("displayName", name) })
        .put("newUser", true)

    @Test fun parsesLoginResponse() {
        val tokens = AuthTokens.parse(response())
        assertEquals(access, tokens.accessToken)
        assertEquals(Instant.parse("2026-11-13T05:00:00Z"), tokens.accessExpiresAt)
        assertEquals("22222222-2222-4222-8222-222222222222", tokens.userId)
        assertEquals("합성 사용자", tokens.displayName)
    }

    @Test fun roundTripsThroughStoredJson() {
        val tokens = AuthTokens.parse(response())
        assertEquals(tokens, AuthTokens.parse(tokens.toJson()))
    }

    @Test fun missingNicknameFallsBack() {
        assertEquals("꼭꼭 사용자", AuthTokens.parse(response(name = null)).displayName)
    }

    @Test fun rejectsUnexpectedTokenFormats() {
        assertThrows(IllegalArgumentException::class.java) { AuthTokens.parse(response(accessToken = "kakao-provider-token")) }
        assertThrows(IllegalArgumentException::class.java) { AuthTokens.parse(response(refreshToken = access)) }
    }

    @Test fun toStringNeverContainsTokens() {
        val text = AuthTokens.parse(response()).toString()
        assertFalse(text.contains(access) || text.contains(refresh))
    }
}
