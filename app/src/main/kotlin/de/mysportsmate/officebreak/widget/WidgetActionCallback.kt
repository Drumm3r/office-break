package de.mysportsmate.officebreak.widget

import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import de.mysportsmate.officebreak.MainActivity
import de.mysportsmate.officebreak.data.SettingsRepository
import de.mysportsmate.officebreak.data.dataStore
import de.mysportsmate.officebreak.service.DefaultTimerServiceController
import kotlinx.coroutines.flow.first

class StartTimerAction : ActionCallback {

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val prefs = context.dataStore.data.first()
        val hours = prefs[SettingsRepository.KEY_TIMER_HOURS] ?: SettingsRepository.DEFAULT_HOURS
        val minutes = prefs[SettingsRepository.KEY_TIMER_MINUTES] ?: SettingsRepository.DEFAULT_MINUTES
        val totalSeconds = (hours * 3600L) + (minutes * 60L)

        if (totalSeconds > 0) {
            val language = prefs[SettingsRepository.KEY_LANGUAGE] ?: SettingsRepository.LANGUAGE_SYSTEM
            DefaultTimerServiceController(context).startTimer(totalSeconds, language, freestyle = false)
        }

        val activityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            setPackage(context.packageName)
        }
        context.startActivity(activityIntent)
    }
}
