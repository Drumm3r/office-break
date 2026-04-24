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
import androidx.compose.ui.graphics.vector.ImageVector

fun iconForName(name: String): ImageVector {
    return when (name) {
        "EmojiEvents" -> Icons.Default.EmojiEvents
        "LocalFireDepartment" -> Icons.Default.LocalFireDepartment
        "FitnessCenter" -> Icons.Default.FitnessCenter
        "Diversity3" -> Icons.Default.Diversity3
        "Shuffle" -> Icons.Default.Shuffle
        "Autorenew" -> Icons.Default.Autorenew
        "Create" -> Icons.Default.Create
        "Today" -> Icons.Default.Today
        "WbSunny" -> Icons.Default.WbSunny
        "NightsStay" -> Icons.Default.NightsStay
        "LunchDining" -> Icons.Default.LunchDining
        "Celebration" -> Icons.Default.Celebration
        "Weekend" -> Icons.Default.Weekend
        "Replay" -> Icons.Default.Replay
        "Star" -> Icons.Default.Star
        "AutoAwesome" -> Icons.Default.AutoAwesome
        else -> Icons.Default.EmojiEvents
    }
}
