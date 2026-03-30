package de.mysportsmate.officebreak.widget

import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import de.mysportsmate.officebreak.MainActivity
import de.mysportsmate.officebreak.data.SettingsRepository
import de.mysportsmate.officebreak.data.dataStore
import de.mysportsmate.officebreak.service.TimerService
import kotlinx.coroutines.flow.first

class StartTimerAction : ActionCallback {

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val prefs = context.dataStore.data.first()
        val hours = prefs[intPreferencesKey("timer_hours")] ?: SettingsRepository.DEFAULT_HOURS
        val minutes = prefs[intPreferencesKey("timer_minutes")] ?: SettingsRepository.DEFAULT_MINUTES
        val totalSeconds = (hours * 3600L) + (minutes * 60L)

        if (totalSeconds > 0) {
            val language = prefs[stringPreferencesKey("language")] ?: SettingsRepository.LANGUAGE_SYSTEM
            val intent = Intent(context, TimerService::class.java).apply {
                action = TimerService.ACTION_START
                putExtra(TimerService.EXTRA_DURATION_SECONDS, totalSeconds)
                putExtra(TimerService.EXTRA_LANGUAGE, language)
            }
            context.startForegroundService(intent)
        }

        val activityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        context.startActivity(activityIntent)
    }
}
