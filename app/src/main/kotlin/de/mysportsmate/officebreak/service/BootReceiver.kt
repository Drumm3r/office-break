package de.mysportsmate.officebreak.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import de.mysportsmate.officebreak.data.AppJson
import de.mysportsmate.officebreak.data.DEFAULT_WEEK_SCHEDULE
import de.mysportsmate.officebreak.data.DaySchedule
import de.mysportsmate.officebreak.data.dataStore
import de.mysportsmate.officebreak.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Reset widget timer state — elapsedRealtime() resets on reboot and
                // TimerService (START_NOT_STICKY) does not survive reboot
                context.dataStore.edit {
                    it[TimerService.KEY_WIDGET_TIMER_STATUS] = "idle"
                    it[TimerService.KEY_WIDGET_TIMER_END_REALTIME] = 0L
                    it[TimerService.KEY_WIDGET_TIMER_TOTAL_SECONDS] = 0L
                    it[TimerService.KEY_WIDGET_TIMER_REMAINING_SECONDS] = 0L
                }
                WidgetUpdater.requestUpdate(context, "idle")

                val prefs = context.dataStore.data.first()
                val enabled = prefs[booleanPreferencesKey("work_schedule_enabled")] ?: false
                if (enabled) {
                    val scheduleJson = prefs[stringPreferencesKey("week_schedule")]
                    val schedule = if (scheduleJson != null) {
                        try {
                            AppJson.decodeFromString<List<DaySchedule>>(scheduleJson)
                        } catch (_: Exception) {
                            DEFAULT_WEEK_SCHEDULE
                        }
                    } else {
                        DEFAULT_WEEK_SCHEDULE
                    }
                    WorkScheduleManager.scheduleNextWorkStartReminder(context, schedule)
                }
            } catch (e: Exception) {
                android.util.Log.e("BootReceiver", "Failed to handle boot", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
