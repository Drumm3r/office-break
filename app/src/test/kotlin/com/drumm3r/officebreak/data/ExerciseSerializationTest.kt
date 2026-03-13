package com.drumm3r.officebreak.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `serialize and deserialize exercise roundtrip`() {
        val exercise = Exercise(name = "Push Ups", isEnabled = true)
        val encoded = json.encodeToString(exercise)
        val decoded = json.decodeFromString<Exercise>(encoded)

        assertEquals(exercise, decoded)
    }

    @Test
    fun `deserialize with missing isEnabled uses default true`() {
        val rawJson = """{"name":"Squats"}"""
        val exercise = json.decodeFromString<Exercise>(rawJson)

        assertEquals("Squats", exercise.name)
        assertTrue(exercise.isEnabled)
    }

    @Test
    fun `serialize list of exercises roundtrip`() {
        val exercises = listOf(
            Exercise(name = "Push Ups", isEnabled = true),
            Exercise(name = "Squats", isEnabled = false),
        )
        val encoded = json.encodeToString(exercises)
        val decoded = json.decodeFromString<List<Exercise>>(encoded)

        assertEquals(exercises, decoded)
    }

    @Test
    fun `deserialize with unknown keys is ignored`() {
        val rawJson = """{"name":"Lunges","isEnabled":true,"unknownField":"value"}"""
        val exercise = json.decodeFromString<Exercise>(rawJson)

        assertEquals("Lunges", exercise.name)
        assertTrue(exercise.isEnabled)
    }

    @Test
    fun `serialized json uses SerialName keys`() {
        val exercise = Exercise(name = "Plank", isEnabled = false)
        val encoded = json.encodeToString(exercise)

        assertTrue(encoded.contains("\"name\""))
        assertTrue(encoded.contains("\"isEnabled\""))
    }
}
