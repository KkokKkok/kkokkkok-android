package com.kkokkkok.android.location

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Synthetic /api/places responses: login mode carries the owner's point, installation mode does not. */
class PlaceResponseTest {
    private fun parse(json: String) = BackendSensitiveLocationRepository.serverPoint(JSONObject(json))

    @Test fun readsOwnerPointAndAddressInLoginMode() {
        val point = parse("""{"placeId":"p1","name":"합성 집","latitude":37.5001,"longitude":127.0001,"address":"서울 합성구 꼭꼭로 12"}""")!!
        assertEquals(37.5001, point.latitude.toDouble(), 1e-9)
        assertEquals(127.0001, point.longitude.toDouble(), 1e-9)
        assertEquals("서울 합성구 꼭꼭로 12", point.address)
    }

    @Test fun keepsPointWhenAddressIsMissing() {
        val point = parse("""{"placeId":"p1","latitude":37.5,"longitude":127.0,"address":null}""")!!
        assertNull(point.address)
    }

    @Test fun installationModeResponseHasNoPoint() {
        assertNull(parse("""{"placeId":"p1","name":"합성 집","category":"home","radiusMeters":300}"""))
        assertNull(parse("""{"placeId":"p1","latitude":null,"longitude":127.0}"""))
        assertNull(parse("""{"placeId":"p1","latitude":123.0,"longitude":127.0}"""))
    }
}
