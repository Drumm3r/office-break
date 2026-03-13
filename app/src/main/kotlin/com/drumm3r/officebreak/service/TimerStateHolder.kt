package com.drumm3r.officebreak.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TimerStateHolder {

    private val _state = MutableStateFlow<TimerState>(TimerState.Idle)
    val state: StateFlow<TimerState> = _state.asStateFlow()

    fun update(newState: TimerState) {
        _state.value = newState
    }

    companion object {
        val instance = TimerStateHolder()
    }
}
