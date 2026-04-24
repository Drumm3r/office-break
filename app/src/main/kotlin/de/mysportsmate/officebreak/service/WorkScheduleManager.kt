package de.mysportsmate.officebreak.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import de.mysportsmate.officebreak.data.DaySchedule
import de.mysportsmate.officebreak.data.resolveEffectiveSchedule
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object WorkScheduleManager {

    private const val TAG = "WorkScheduleManager"
    private const val REQUEST_CODE = 1001
    internal const val REMINDER_DELAY_MINUTES = 10

    /**
     * Pure function: given a weekly schedule and a reference time, returns the
     * Calendar instant of the next work-start reminder (workStart + delay) within
     * the next 7 days, or null if no enabled day found.
     */
    internal fun computeNextWorkStartTime(
        schedule: List<DaySchedule>,
        now: Calendar,
        reminderDelayMinutes: Int = REMINDER_DELAY_MINUTES,
    ): Calendar? {
        val todayDow = (now.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Mon=0..Sun=6

        for (offset in 0..6) {
            val dayIndex = (todayDow + offset) % 7
            val effective = resolveEffectiveSchedule(schedule, dayIndex) ?: continue

            val alarmTime = (now.clone() as Calendar).apply {
                if (offset > 0) add(Calendar.DAY_OF_YEAR, offset)
                set(Calendar.HOUR_OF_DAY, effective.workStartHour)
                set(Calendar.MINUTE, effective.workStartMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.MINUTE, reminderDelayMinutes)
            }

            if (alarmTime.after(now)) return alarmTime
        }
        return null
    }

    fun scheduleNextWorkStartReminder(context: Context, schedule: List<DaySchedule>) {
        val now = Calendar.getInstance()
        val alarmTime = computeNextWorkStartTime(schedule, now)
        if (alarmTime == null) {
            Log.w(TAG, "No future alarm found in the next 7 days")
            return
        }
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        Log.d(TAG, "Next alarm at ${fmt.format(alarmTime.time)}")
        scheduleAlarm(context, alarmTime.timeInMillis)
    }

    fun cancelWorkStartReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildPendingIntent(context)
        alarmManager.cancel(pendingIntent)
    }

    private fun scheduleAlarm(context: Context, timeMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = buildPendingIntent(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Log.w(TAG, "Exact alarm permission not granted, using inexact alarm")
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
            return
        }

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                timeMillis,
                pendingIntent,
            )
            Log.d(TAG, "Exact alarm scheduled successfully")
        } catch (e: SecurityException) {
            Log.w(TAG, "Cannot schedule exact alarm, falling back to inexact", e)
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
        }
    }

    private fun buildPendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, WorkScheduleReceiver::class.java).apply {
                setPackage(context.packageName)
            },
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
