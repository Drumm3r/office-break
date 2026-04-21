package de.mysportsmate.officebreak.service

import de.mysportsmate.officebreak.data.DaySchedule
import java.time.LocalTime

sealed interface TimerTickDecision {
    data object Continue : TimerTickDecision
    data object WorkEnded : TimerTickDecision
    data class Pause(val lunchEnd: LocalTime) : TimerTickDecision
}

object TimerPauseResolver {

    fun isTimeInRange(time: LocalTime, start: LocalTime, end: LocalTime): Boolean {
        return if (!start.isAfter(end)) {
            !time.isBefore(start) && time.isBefore(end)
        } else {
            !time.isBefore(start) || time.isBefore(end)
        }
    }

    /**
     * Pure: decide what the tick loop should do given the current time and today's schedule.
     * Null schedule means no work-schedule gating.
     */
    fun decide(now: LocalTime, today: DaySchedule?): TimerTickDecision {
        if (today == null) return TimerTickDecision.Continue

        val workStart = LocalTime.of(today.workStartHour, today.workStartMinute)
        val workEnd = LocalTime.of(today.workEndHour, today.workEndMinute)
        val lunchStart = LocalTime.of(today.lunchStartHour, today.lunchStartMinute)
        val lunchEnd = LocalTime.of(today.lunchEndHour, today.lunchEndMinute)

        if (isTimeInRange(now, workEnd, workStart)) return TimerTickDecision.WorkEnded
        if (isTimeInRange(now, lunchStart, lunchEnd)) return TimerTickDecision.Pause(lunchEnd)
        return TimerTickDecision.Continue
    }
}
