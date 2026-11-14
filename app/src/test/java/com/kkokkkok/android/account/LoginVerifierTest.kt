package com.kkokkkok.android.account

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginVerifierTest {
    @Test fun challengeMatchesRfc7636Example() {
        assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
            LoginVerifier.challenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"))
    }

    @Test fun verifiersAreRandomBase64UrlOfAllowedLength() {
        val first = LoginVerifier.create()
        assertTrue(first.matches(Regex("[A-Za-z0-9_-]{43}")))
        assertNotEquals(first, LoginVerifier.create())
        assertTrue(LoginVerifier.challenge(first).matches(Regex("[A-Za-z0-9_-]{43}")))
    }
}
