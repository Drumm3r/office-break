package de.mysportsmate.officebreak.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Diversity3
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Weekend
import org.junit.Assert.assertEquals
import org.junit.Test

class AchievementIconTest {

    @Test
    fun `iconForName returns EmojiEvents for EmojiEvents`() {
        assertEquals(Icons.Default.EmojiEvents, iconForName("EmojiEvents"))
    }

    @Test
    fun `iconForName returns LocalFireDepartment`() {
        assertEquals(Icons.Default.LocalFireDepartment, iconForName("LocalFireDepartment"))
    }

    @Test
    fun `iconForName returns FitnessCenter`() {
        assertEquals(Icons.Default.FitnessCenter, iconForName("FitnessCenter"))
    }

    @Test
    fun `iconForName returns Diversity3 for Diversity3`() {
        assertEquals(Icons.Default.Diversity3, iconForName("Diversity3"))
    }

    @Test
    fun `iconForName returns AutoAwesome for AutoAwesome`() {
        assertEquals(Icons.Default.AutoAwesome, iconForName("AutoAwesome"))
    }

    @Test
    fun `iconForName returns Shuffle`() {
        assertEquals(Icons.Default.Shuffle, iconForName("Shuffle"))
    }

    @Test
    fun `iconForName returns Autorenew`() {
        assertEquals(Icons.Default.Autorenew, iconForName("Autorenew"))
    }

    @Test
    fun `iconForName returns Create`() {
        assertEquals(Icons.Default.Create, iconForName("Create"))
    }

    @Test
    fun `iconForName returns Today`() {
        assertEquals(Icons.Default.Today, iconForName("Today"))
    }

    @Test
    fun `iconForName returns WbSunny`() {
        assertEquals(Icons.Default.WbSunny, iconForName("WbSunny"))
    }

    @Test
    fun `iconForName returns NightsStay`() {
        assertEquals(Icons.Default.NightsStay, iconForName("NightsStay"))
    }

    @Test
    fun `iconForName returns LunchDining for LunchDining`() {
        assertEquals(Icons.Default.LunchDining, iconForName("LunchDining"))
    }

    @Test
    fun `iconForName returns Celebration`() {
        assertEquals(Icons.Default.Celebration, iconForName("Celebration"))
    }

    @Test
    fun `iconForName returns Weekend`() {
        assertEquals(Icons.Default.Weekend, iconForName("Weekend"))
    }

    @Test
    fun `iconForName returns Replay`() {
        assertEquals(Icons.Default.Replay, iconForName("Replay"))
    }

    @Test
    fun `iconForName returns Star`() {
        assertEquals(Icons.Default.Star, iconForName("Star"))
    }

    @Test
    fun `iconForName returns EmojiEvents as fallback for unknown name`() {
        assertEquals(Icons.Default.EmojiEvents, iconForName("UnknownIcon"))
    }

    @Test
    fun `iconForName returns EmojiEvents as fallback for empty string`() {
        assertEquals(Icons.Default.EmojiEvents, iconForName(""))
    }

    @Test
    fun `all known icon names return distinct mappings where expected`() {
        val knownNames = listOf(
            "EmojiEvents", "LocalFireDepartment", "FitnessCenter", "Shuffle",
            "Autorenew", "Create", "WbSunny", "NightsStay", "Celebration",
            "Weekend", "Replay", "Star",
        )
        val icons = knownNames.map { iconForName(it) }
        assertEquals("All distinct icon names should map to distinct icons", knownNames.size, icons.toSet().size)
    }
}
