package de.mysportsmate.officebreak.widget

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll

object WidgetUpdater {

    val KEY_TIMER_STATUS = stringPreferencesKey("timer_status")
    val KEY_TIMER_END_REALTIME = longPreferencesKey("timer_end_realtime")
    val KEY_TIMER_REMAINING_SECONDS = longPreferencesKey("timer_remaining_seconds")

    suspend fun requestUpdate(
        context: Context,
        timerStatus: String = "",
        remainingSeconds: Long = 0L,
        totalSeconds: Long = 0L,
    ) {
        try {
            val appContext = context.applicationContext
            val manager = GlanceAppWidgetManager(appContext)
            val widget = OfficeBreakWidget()
            val glanceIds = manager.getGlanceIds(widget.javaClass)

            val endRealtime = WidgetTimerState.computeEndRealtime(
                timerStatus = timerStatus,
                remainingSeconds = remainingSeconds,
                nowRealtime = SystemClock.elapsedRealtime(),
            )

            for (id in glanceIds) {
                updateAppWidgetState(appContext, id) { prefs ->
                    if (timerStatus.isNotEmpty()) {
                        prefs[KEY_TIMER_STATUS] = timerStatus
                        prefs[KEY_TIMER_END_REALTIME] = endRealtime
                        prefs[KEY_TIMER_REMAINING_SECONDS] = remainingSeconds
                    }
                }
            }

            widget.updateAll(appContext)
        } catch (e: Throwable) {
            Log.e("WidgetUpdater", "Widget update failed", e)
        }
    }
}
