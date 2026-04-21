package de.mysportsmate.officebreak.widget

import android.content.Context
import android.os.SystemClock
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import de.mysportsmate.officebreak.data.AppJson
import de.mysportsmate.officebreak.data.BreakRecord
import de.mysportsmate.officebreak.data.SettingsRepository
import de.mysportsmate.officebreak.data.StatsRepository
import de.mysportsmate.officebreak.data.StatsSnapshot
import de.mysportsmate.officebreak.data.dataStore
import de.mysportsmate.officebreak.data.statsDataStore
import de.mysportsmate.officebreak.locale.LocaleHelper
import kotlinx.coroutines.flow.first
import java.time.LocalDate

class OfficeBreakWidget : GlanceAppWidget() {

    private val json = AppJson

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Load stats data before provideContent (runs once)
        val statsPrefs = context.statsDataStore.data.first()

        val snapshot = statsPrefs[StatsRepository.KEY_STATS_SNAPSHOT]?.let {
            try {
                json.decodeFromString<StatsSnapshot>(it)
            } catch (_: Exception) {
                StatsSnapshot()
            }
        } ?: StatsSnapshot()

        val breakRecords = statsPrefs[StatsRepository.KEY_BREAK_RECORDS]?.let {
            try {
                json.decodeFromString<List<BreakRecord>>(it)
            } catch (_: Exception) {
                emptyList()
            }
        } ?: emptyList()

        val today = LocalDate.now().toString()
        val todayBreaks = breakRecords.count { it.dateString == today }

        val settingsPrefs = context.dataStore.data.first()
        val language = settingsPrefs[SettingsRepository.KEY_LANGUAGE] ?: SettingsRepository.LANGUAGE_SYSTEM
        val localizedContext = LocaleHelper.createLocalizedContext(context, language)

        provideContent {
            // Read timer state from Glance state (reactive — recomposes on change)
            val glanceState = currentState<Preferences>()
            val storedStatus = glanceState[WidgetUpdater.KEY_TIMER_STATUS] ?: WidgetTimerState.STATUS_IDLE
            val endRealtime = glanceState[WidgetUpdater.KEY_TIMER_END_REALTIME] ?: 0L
            val storedRemaining = glanceState[WidgetUpdater.KEY_TIMER_REMAINING_SECONDS] ?: 0L

            val display = WidgetTimerState.resolveDisplay(
                storedStatus = storedStatus,
                endRealtime = endRealtime,
                storedRemaining = storedRemaining,
                nowRealtime = SystemClock.elapsedRealtime(),
            )

            WidgetContent(
                context = localizedContext,
                todayBreaks = todayBreaks,
                currentStreak = snapshot.currentStreakDays,
                timerStatus = display.status,
                remainingSeconds = display.remainingSeconds,
            )
        }
    }
}
