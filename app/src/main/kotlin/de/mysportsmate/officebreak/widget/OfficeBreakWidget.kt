package de.mysportsmate.officebreak.widget

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import de.mysportsmate.officebreak.data.BreakRecord
import de.mysportsmate.officebreak.data.SettingsRepository
import de.mysportsmate.officebreak.data.StatsSnapshot
import de.mysportsmate.officebreak.data.dataStore
import de.mysportsmate.officebreak.data.statsDataStore
import de.mysportsmate.officebreak.locale.LocaleHelper
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.time.LocalDate

class OfficeBreakWidget : GlanceAppWidget() {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val statsPrefs = context.statsDataStore.data.first()
        val settingsPrefs = context.dataStore.data.first()

        val snapshot = statsPrefs[stringPreferencesKey("stats_snapshot")]?.let {
            try {
                json.decodeFromString<StatsSnapshot>(it)
            } catch (_: Exception) {
                StatsSnapshot()
            }
        } ?: StatsSnapshot()

        val breakRecords = statsPrefs[stringPreferencesKey("break_records")]?.let {
            try {
                json.decodeFromString<List<BreakRecord>>(it)
            } catch (_: Exception) {
                emptyList()
            }
        } ?: emptyList()

        val today = LocalDate.now().toString()
        val todayBreaks = breakRecords.count { it.dateString == today }

        val timerStatus = settingsPrefs[stringPreferencesKey("widget_timer_status")] ?: "idle"

        val language = settingsPrefs[stringPreferencesKey("language")] ?: SettingsRepository.LANGUAGE_SYSTEM
        val localizedContext = LocaleHelper.createLocalizedContext(context, language)

        provideContent {
            WidgetContent(
                context = localizedContext,
                todayBreaks = todayBreaks,
                currentStreak = snapshot.currentStreakDays,
                timerStatus = timerStatus,
            )
        }
    }
}
