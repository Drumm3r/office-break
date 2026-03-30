package de.mysportsmate.officebreak.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll

object WidgetUpdater {

    suspend fun requestUpdate(context: Context) {
        try {
            OfficeBreakWidget().updateAll(context.applicationContext)
        } catch (e: Exception) {
            Log.e("WidgetUpdater", "Widget update failed", e)
        }
    }
}
