package de.mysportsmate.officebreak.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import de.mysportsmate.officebreak.data.AppJson
import de.mysportsmate.officebreak.data.DEFAULT_WEEK_SCHEDULE
import de.mysportsmate.officebreak.data.DaySchedule
import de.mysportsmate.officebreak.data.dataStore
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
                val prefs = context.dataStore.data.first()
                val enabled = prefs[androidx.datastore.preferences.core.booleanPreferencesKey("work_schedule_enabled")] ?: false
                if (enabled) {
                    val scheduleJson = prefs[androidx.datastore.preferences.core.stringPreferencesKey("week_schedule")]
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
                android.util.Log.e("BootReceiver", "Failed to reschedule alarm after boot", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
