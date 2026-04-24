package de.mysportsmate.officebreak.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TimerStateHolder {

    private val _state = MutableStateFlow<TimerState>(TimerState.Idle)
    val state: StateFlow<TimerState> = _state.asStateFlow()

    private val _isMusicPlaying = MutableStateFlow(false)
    val isMusicPlaying: StateFlow<Boolean> = _isMusicPlaying.asStateFlow()

    fun update(newState: TimerState) {
        _state.value = newState
        if (newState is TimerState.Idle) {
            _isMusicPlaying.value = false
        }
    }

    fun updateMusicPlaying(playing: Boolean) {
        _isMusicPlaying.value = playing
    }

    companion object {
        val instance = TimerStateHolder()
    }
}
