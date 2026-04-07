package de.mysportsmate.officebreak.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import de.mysportsmate.officebreak.data.DaySchedule
import de.mysportsmate.officebreak.data.resolveEffectiveSchedule
import java.util.Calendar

object WorkScheduleManager {

    private const val REQUEST_CODE = 1001

    fun scheduleNextWorkStartReminder(context: Context, schedule: List<DaySchedule>) {
        val now = Calendar.getInstance()
        val todayDow = (now.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Convert to Mon=0..Sun=6

        for (offset in 0..6) {
            val dayIndex = (todayDow + offset) % 7
            val effective = resolveEffectiveSchedule(schedule, dayIndex) ?: continue

            val alarmTime = Calendar.getInstance().apply {
                if (offset > 0) add(Calendar.DAY_OF_YEAR, offset)
                set(Calendar.HOUR_OF_DAY, effective.workStartHour)
                set(Calendar.MINUTE, effective.workStartMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (alarmTime.after(now)) {
                scheduleAlarm(context, alarmTime.timeInMillis)
                return
            }
        }
    }

    fun cancelWorkStartReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildPendingIntent(context)
        alarmManager.cancel(pendingIntent)
    }

    private fun scheduleAlarm(context: Context, timeMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                timeMillis,
                buildPendingIntent(context),
            )
        } catch (e: SecurityException) {
            android.util.Log.w("WorkScheduleManager", "Cannot schedule exact alarm", e)
        }
    }

    private fun buildPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, WorkScheduleReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
