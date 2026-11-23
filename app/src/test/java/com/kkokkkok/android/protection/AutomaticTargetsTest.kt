package com.kkokkkok.android.protection

import com.kkokkkok.android.analysis.parseAnalysisResult
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Synthetic analysis responses only. */
class AutomaticTargetsTest {
    private val result = parseAnalysisResult(JSONObject("""{"status":"completed","riskSummary":{"score":60},
        "riskFactors":[
          {"id":"sign","type":"text","ruleClass":"identifier","modelRisk":"high","bbox":[0.1,0.1,0.3,0.2]},
          {"id":"facade","type":"building","ruleClass":"structure","modelRisk":"medium","bbox":[0.0,0.0,0.9,0.9]},
          {"id":"gate","type":"object","ruleClass":"structure","modelRisk":"medium","bbox":[0.4,0.4,0.6,0.7]}]}"""))

    @Test fun buildingAndStructureCluesAreMarkedStructural() {
        assertEquals(listOf(false, true, true), result.clues.map { it.structural })
    }

    @Test fun automaticEditingPicksOnlyWritingAndObjects() {
        assertEquals(setOf("sign"), automaticTargets(result.clues))
    }

    @Test fun onlyStructuralCluesMeanTheUserChooses() {
        assertTrue(automaticTargets(result.clues.filter { it.structural }).isEmpty())
    }
}
