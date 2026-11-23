package com.example.overlay

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class OverlaySharedState(
    val aimbotEnabled: Boolean = false,
    val locationEspEnabled: Boolean = false,
    val isOverlayRunning: Boolean = false,
    val isExpanded: Boolean = false
)

object OverlayPanelStateHolder {
    private val _state = MutableStateFlow(OverlaySharedState())
    val state: StateFlow<OverlaySharedState> = _state.asStateFlow()

    fun toggleAimbot(): Boolean {
        var nextValue = false
        _state.update { current ->
            nextValue = !current.aimbotEnabled
            current.copy(aimbotEnabled = nextValue)
        }
        return nextValue
    }

    fun toggleLocationEsp(): Boolean {
        var nextValue = false
        _state.update { current ->
            nextValue = !current.locationEspEnabled
            current.copy(locationEspEnabled = nextValue)
        }
        return nextValue
    }

    fun resetToggles() {
        _state.update { current ->
            current.copy(
                aimbotEnabled = false,
                locationEspEnabled = false
            )
        }
    }

    fun setOverlayRunning(running: Boolean) {
        _state.update { current ->
            current.copy(
                isOverlayRunning = running,
                isExpanded = if (running) current.isExpanded else false
            )
        }
    }

    fun setExpanded(expanded: Boolean) {
        _state.update { current ->
            current.copy(isExpanded = expanded)
        }
    }

    fun toggleExpanded() {
        _state.update { current ->
            current.copy(isExpanded = !current.isExpanded)
        }
    }
}
