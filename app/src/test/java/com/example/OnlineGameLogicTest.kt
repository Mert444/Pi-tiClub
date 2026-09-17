package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnlineGameLogicTest {

    @Test
    fun testCardPointsCalculation() {
        fun getPoints(suit: String, rank: String): Int {
            return when {
                rank == "ACE" -> 1
                rank == "JACK" -> 1
                suit == "DIAMONDS" && rank == "TEN" -> 3
                suit == "CLUBS" && rank == "TWO" -> 2
                else -> 0
            }
        }

        assertEquals(1, getPoints("HEARTS", "ACE"))
        assertEquals(1, getPoints("SPADES", "JACK"))
        assertEquals(3, getPoints("DIAMONDS", "TEN"))
        assertEquals(2, getPoints("CLUBS", "TWO"))
        assertEquals(0, getPoints("HEARTS", "TEN"))
        assertEquals(0, getPoints("SPADES", "SEVEN"))
    }

    @Test
    fun testExact101ScoringRule() {
        val targetExact = 101

        fun calculateScore(currentTotal: Int, roundScore: Int): Pair<Int, Boolean> {
            val candidate = currentTotal + roundScore
            return when {
                candidate == targetExact -> Pair(targetExact, true) // Hit exact 101 -> Match won
                candidate > targetExact -> Pair(50, false) // Overshot -> Reset to 50
                else -> Pair(candidate, false) // Under 101
            }
        }

        // Test exact 101 hit
        val exactResult = calculateScore(91, 10)
        assertEquals(101, exactResult.first)
        assertTrue(exactResult.second)

        // Test overshooting 101 -> penalty resets to 50
        val overshotResult = calculateScore(95, 10)
        assertEquals(50, overshotResult.first)
        assertFalse(overshotResult.second)

        // Test normal scoring below 101
        val normalResult = calculateScore(40, 16)
        assertEquals(56, normalResult.first)
        assertFalse(normalResult.second)
    }

    @Test
    fun testPistiDetection() {
        fun checkPisti(centerPileSize: Int, playedRank: String, topRank: String): Boolean {
            return centerPileSize == 1 && playedRank == topRank
        }

        // 1 card on pile and matching rank played -> Pişti!
        assertTrue(checkPisti(1, "SEVEN", "SEVEN"))

        // Multiple cards on pile -> regular capture, not Pişti
        assertFalse(checkPisti(2, "SEVEN", "SEVEN"))

        // Different ranks -> no capture/pisti
        assertFalse(checkPisti(1, "EIGHT", "SEVEN"))
    }
}
