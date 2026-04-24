package de.mysportsmate.officebreak.data

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
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
    val defaultMode: ExerciseMode = ExerciseMode.HOME_WORKOUT,
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

fun DaySchedule.clamp(): DaySchedule = copy(
    workStartHour = workStartHour.coerceIn(0, 23),
    workStartMinute = workStartMinute.coerceIn(0, 59),
    workEndHour = workEndHour.coerceIn(0, 23),
    workEndMinute = workEndMinute.coerceIn(0, 59),
    lunchStartHour = lunchStartHour.coerceIn(0, 23),
    lunchStartMinute = lunchStartMinute.coerceIn(0, 59),
    lunchEndHour = lunchEndHour.coerceIn(0, 23),
    lunchEndMinute = lunchEndMinute.coerceIn(0, 59),
)

fun DaySchedule.validated(): DaySchedule {
    val workStartMin = workStartHour * 60 + workStartMinute
    val workEndMin = workEndHour * 60 + workEndMinute
    if (workStartMin == workEndMin) return this

    val lunchStartMin: Int
    val lunchEndMin: Int

    if (workStartMin < workEndMin) {
        lunchStartMin = (lunchStartHour * 60 + lunchStartMinute).coerceIn(workStartMin, workEndMin - 1)
        lunchEndMin = (lunchEndHour * 60 + lunchEndMinute).coerceIn(lunchStartMin + 1, workEndMin)
    } else {
        // Night shift: work spans midnight, clamp lunch within the work range
        val rawLunchStart = lunchStartHour * 60 + lunchStartMinute
        val rawLunchEnd = lunchEndHour * 60 + lunchEndMinute
        lunchStartMin = if (rawLunchStart >= workStartMin || rawLunchStart < workEndMin) {
            rawLunchStart
        } else {
            workStartMin
        }
        lunchEndMin = if (rawLunchEnd > lunchStartMin || (rawLunchEnd > 0 && rawLunchEnd <= workEndMin)) {
            rawLunchEnd
        } else {
            (lunchStartMin + 60) % (24 * 60)
        }
    }

    return copy(
        lunchStartHour = lunchStartMin / 60,
        lunchStartMinute = lunchStartMin % 60,
        lunchEndHour = lunchEndMin / 60,
        lunchEndMinute = lunchEndMin % 60,
    )
}

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
                defaultMode = prev.defaultMode,
            )
        }
    }
    return day
}
