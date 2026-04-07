package de.mysportsmate.officebreak.data

import kotlinx.serialization.Serializable

@Serializable
data class DaySchedule(
    val enabled: Boolean = false,
    val linked: Boolean = true,
    val workStartHour: Int = 8,
    val workStartMinute: Int = 0,
    val workEndHour: Int = 17,
    val workEndMinute: Int = 0,
    val lunchStartHour: Int = 12,
    val lunchStartMinute: Int = 0,
    val lunchEndHour: Int = 13,
    val lunchEndMinute: Int = 0,
)

val DEFAULT_WEEK_SCHEDULE: List<DaySchedule> = listOf(
    DaySchedule(enabled = true, linked = false),   // Monday
    DaySchedule(enabled = true, linked = true),     // Tuesday
    DaySchedule(enabled = true, linked = true),     // Wednesday
    DaySchedule(enabled = true, linked = true),     // Thursday
    DaySchedule(enabled = true, linked = true),     // Friday
    DaySchedule(enabled = false, linked = false),   // Saturday
    DaySchedule(enabled = false, linked = false),   // Sunday
)

fun resolveEffectiveSchedule(schedule: List<DaySchedule>, dayIndex: Int): DaySchedule? {
    if (dayIndex !in schedule.indices) return null
    val day = schedule[dayIndex]
    if (!day.enabled) return null
    if (!day.linked) return day

    for (i in 1..6) {
        val prev = schedule[(dayIndex - i + 7) % 7]
        if (prev.enabled && !prev.linked) {
            return day.copy(
                workStartHour = prev.workStartHour,
                workStartMinute = prev.workStartMinute,
                workEndHour = prev.workEndHour,
                workEndMinute = prev.workEndMinute,
                lunchStartHour = prev.lunchStartHour,
                lunchStartMinute = prev.lunchStartMinute,
                lunchEndHour = prev.lunchEndHour,
                lunchEndMinute = prev.lunchEndMinute,
            )
        }
    }
    return day
}
