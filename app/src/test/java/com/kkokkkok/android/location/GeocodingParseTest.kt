package com.kkokkkok.android.location

import com.kkokkkok.android.ui.map.zoomForRadius
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Synthetic backend responses; verifies the app reads latitude/longitude (not Kakao x/y) from /api/geocoding. */
class GeocodingParseTest {
    @Test fun parsesSearchResultsAsLatitudeLongitude() {
        val json = JSONObject("""{"query":"꼭꼭로","coordinateSystem":"WGS84","results":[
            {"type":"address","name":"서울 합성구 꼭꼭로 12","address":"서울 합성구 꼭꼭로 12","latitude":37.5001,"longitude":127.0001},
            {"type":"place","name":"합성 카페","address":null,"latitude":37.5002,"longitude":127.0002},
            {"type":"place","name":"잘못된 좌표","latitude":127.0,"longitude":37.0},
            {"type":"place","name":"","latitude":37.1,"longitude":127.1}]}""")
        val results = BackendGeocodingRepository.parseSearch(json)
        assertEquals(2, results.size)
        assertEquals(GeoPoint(37.5001, 127.0001), results[0].point)
        assertTrue(results[0].isAddress)
        assertNull(results[1].address)
    }

    @Test fun emptyResultsAreEmpty() {
        assertTrue(BackendGeocodingRepository.parseSearch(JSONObject("""{"results":[]}""")).isEmpty())
    }

    @Test fun parsesStoredCoordinatesOnlyWhenValid() {
        assertEquals(GeoPoint(37.5, 127.0), GeoPoint.parse("37.5", "127.0"))
        assertNull(GeoPoint.parse("", "127.0"))
        assertNull(GeoPoint.parse("127.0", "37.5".replace("37.5", "200")))
    }

    @Test fun largerRadiusZoomsOut() {
        assertTrue(zoomForRadius(100) > zoomForRadius(300))
        assertTrue(zoomForRadius(300) > zoomForRadius(500))
        assertTrue(zoomForRadius(500) > zoomForRadius(1000))
    }
}
