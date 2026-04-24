package de.mysportsmate.officebreak.widget

data class WidgetTimerDisplay(val status: String, val remainingSeconds: Long)

object WidgetTimerState {

    const val STATUS_IDLE = "idle"
    const val STATUS_RUNNING = "running"
    const val STATUS_PAUSED = "paused"
    const val STATUS_EXPIRED = "expired"
    const val STATUS_WORK_ENDED = "work_ended"

    fun computeEndRealtime(
        timerStatus: String,
        remainingSeconds: Long,
        nowRealtime: Long,
    ): Long {
        return if (timerStatus == STATUS_RUNNING && remainingSeconds > 0) {
            nowRealtime + remainingSeconds * 1000L
        } else {
            0L
        }
    }

    fun resolveDisplay(
        storedStatus: String,
        endRealtime: Long,
        storedRemaining: Long,
        nowRealtime: Long,
    ): WidgetTimerDisplay {
        val remainingMs = endRealtime - nowRealtime

        return when {
            storedStatus == STATUS_RUNNING && endRealtime > 0 && remainingMs > 0 ->
                WidgetTimerDisplay(STATUS_RUNNING, remainingMs / 1000)
            storedStatus == STATUS_RUNNING && (endRealtime == 0L || remainingMs <= 0) ->
                WidgetTimerDisplay(STATUS_IDLE, 0L)
            storedStatus == STATUS_PAUSED && storedRemaining > 0 ->
                WidgetTimerDisplay(STATUS_PAUSED, storedRemaining)
            else -> WidgetTimerDisplay(storedStatus, 0L)
        }
    }
}
