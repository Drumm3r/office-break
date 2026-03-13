package com.drumm3r.officebreak

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.content.getSystemService

class OfficeBreakApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val timerChannel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notification_channel_description)
            setShowBadge(false)
        }

        val alertChannel = NotificationChannel(
            ALERT_CHANNEL_ID,
            getString(R.string.notification_channel_alert_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = getString(R.string.notification_channel_alert_description)
        }

        getSystemService<NotificationManager>()?.createNotificationChannels(
            listOf(timerChannel, alertChannel),
        )
    }

    companion object {
        const val CHANNEL_ID = "timer_channel"
        const val ALERT_CHANNEL_ID = "timer_alert_channel"
    }
}
