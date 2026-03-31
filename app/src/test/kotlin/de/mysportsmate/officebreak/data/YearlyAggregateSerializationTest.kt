package de.mysportsmate.officebreak.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class YearlyAggregateSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `roundtrip with all fields populated`() {
        val aggregate = YearlyAggregate(
            year = 2025,
            totalBreaks = 200,
            totalReps = 2000,
            exerciseCounts = mapOf("Push Ups" to 100, "Squats" to 100),
            exerciseReps = mapOf("Push Ups" to 1000, "Squats" to 1000),
            activeDays = 150,
        )
        val encoded = json.encodeToString(aggregate)
        val decoded = json.decodeFromString<YearlyAggregate>(encoded)
        assertEquals(aggregate, decoded)
    }

    @Test
    fun `roundtrip with empty maps`() {
        val aggregate = YearlyAggregate(
            year = 2024,
            totalBreaks = 0,
            totalReps = 0,
            exerciseCounts = emptyMap(),
            exerciseReps = emptyMap(),
            activeDays = 0,
        )
        val encoded = json.encodeToString(aggregate)
        val decoded = json.decodeFromString<YearlyAggregate>(encoded)
        assertEquals(aggregate, decoded)
    }

    @Test
    fun `deserialize with unknown keys is ignored`() {
        val rawJson = """
            {
                "year": 2025,
                "totalBreaks": 10,
                "totalReps": 100,
                "exerciseCounts": {},
                "exerciseReps": {},
                "activeDays": 5,
                "newField": true
            }
        """.trimIndent()
        val decoded = json.decodeFromString<YearlyAggregate>(rawJson)
        assertEquals(2025, decoded.year)
        assertEquals(5, decoded.activeDays)
    }
}
