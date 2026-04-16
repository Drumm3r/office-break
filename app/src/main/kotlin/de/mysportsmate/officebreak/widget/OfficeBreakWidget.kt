package de.mysportsmate.officebreak.widget

import android.content.Context
import android.os.SystemClock
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import de.mysportsmate.officebreak.data.AppJson
import de.mysportsmate.officebreak.data.BreakRecord
import de.mysportsmate.officebreak.data.SettingsRepository
import de.mysportsmate.officebreak.data.StatsSnapshot
import de.mysportsmate.officebreak.data.dataStore
import de.mysportsmate.officebreak.data.statsDataStore
import de.mysportsmate.officebreak.locale.LocaleHelper
import de.mysportsmate.officebreak.service.TimerService
import kotlinx.coroutines.flow.first
import java.time.LocalDate

class OfficeBreakWidget : GlanceAppWidget() {

    private val json = AppJson

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

        val storedStatus = settingsPrefs[TimerService.KEY_WIDGET_TIMER_STATUS] ?: "idle"
        val endRealtime = settingsPrefs[TimerService.KEY_WIDGET_TIMER_END_REALTIME] ?: 0L
        val storedRemaining = settingsPrefs[TimerService.KEY_WIDGET_TIMER_REMAINING_SECONDS] ?: 0L

        val now = SystemClock.elapsedRealtime()
        val remainingMs = endRealtime - now

        val (timerStatus, remainingSeconds) = when {
            storedStatus == "running" && endRealtime > 0 && remainingMs > 0 ->
                "running" to (remainingMs / 1000)
            storedStatus == "running" && (endRealtime == 0L || remainingMs <= 0) ->
                "idle" to 0L
            storedStatus == "paused" && storedRemaining > 0 ->
                "paused" to storedRemaining
            else -> storedStatus to 0L
        }

        val language = settingsPrefs[stringPreferencesKey("language")] ?: SettingsRepository.LANGUAGE_SYSTEM
        val localizedContext = LocaleHelper.createLocalizedContext(context, language)

        provideContent {
            WidgetContent(
                context = localizedContext,
                todayBreaks = todayBreaks,
                currentStreak = snapshot.currentStreakDays,
                timerStatus = timerStatus,
                remainingSeconds = remainingSeconds,
            )
        }
    }
}
