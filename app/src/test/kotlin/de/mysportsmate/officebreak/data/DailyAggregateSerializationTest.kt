package de.mysportsmate.officebreak.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyAggregateSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `roundtrip with all fields populated`() {
        val aggregate = DailyAggregate(
            dateString = "2026-03-30",
            totalBreaks = 5,
            totalReps = 50,
            exerciseCounts = mapOf("Push Ups" to 3, "Squats" to 2),
            exerciseReps = mapOf("Push Ups" to 30, "Squats" to 20),
        )
        val encoded = json.encodeToString(aggregate)
        val decoded = json.decodeFromString<DailyAggregate>(encoded)
        assertEquals(aggregate, decoded)
    }

    @Test
    fun `roundtrip with empty maps`() {
        val aggregate = DailyAggregate(
            dateString = "2026-01-01",
            totalBreaks = 0,
            totalReps = 0,
            exerciseCounts = emptyMap(),
            exerciseReps = emptyMap(),
        )
        val encoded = json.encodeToString(aggregate)
        val decoded = json.decodeFromString<DailyAggregate>(encoded)
        assertEquals(aggregate, decoded)
        assertTrue(decoded.exerciseCounts.isEmpty())
    }

    @Test
    fun `deserialize with unknown keys is ignored`() {
        val rawJson = """
            {
                "dateString": "2026-03-30",
                "totalBreaks": 1,
                "totalReps": 10,
                "exerciseCounts": {},
                "exerciseReps": {},
                "futureField": "ignored"
            }
        """.trimIndent()
        val decoded = json.decodeFromString<DailyAggregate>(rawJson)
        assertEquals("2026-03-30", decoded.dateString)
        assertEquals(1, decoded.totalBreaks)
    }
}
