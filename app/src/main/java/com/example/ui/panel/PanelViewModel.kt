package com.example.ui.panel

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.overlay.OverlayPanelStateHolder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PanelBannerKind(@param:StringRes val messageRes: Int) {
    STANDBY(R.string.activity_standby),
    ONE_ACTIVE(R.string.activity_one_active),
    TWO_ACTIVE(R.string.activity_two_active),
    AIMBOT_ON(R.string.activity_aimbot_on),
    AIMBOT_OFF(R.string.activity_aimbot_off),
    ESP_ON(R.string.activity_esp_on),
    ESP_OFF(R.string.activity_esp_off),
    RESET_COMPLETE(R.string.activity_reset_complete),
    OVERLAY_STARTED(R.string.activity_overlay_started),
    OVERLAY_STOPPED(R.string.activity_overlay_stopped)
}

data class PanelUiState(
    val aimbotEnabled: Boolean = false,
    val locationEspEnabled: Boolean = false,
    val isOverlayRunning: Boolean = false,
    val hasOverlayPermission: Boolean = false,
    val bannerKind: PanelBannerKind = PanelBannerKind.STANDBY,
    val isPositiveActivation: Boolean = false,
    val sweepTriggerId: Int = 0,
    val aimbotPopTrigger: Int = 0,
    val espPopTrigger: Int = 0,
    val resetSpinTrigger: Int = 0
)

class PanelViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val initialAimbot = savedStateHandle.get<Boolean>(KEY_AIMBOT)
        ?: OverlayPanelStateHolder.state.value.aimbotEnabled
    private val initialEsp = savedStateHandle.get<Boolean>(KEY_ESP)
        ?: OverlayPanelStateHolder.state.value.locationEspEnabled

    private val _uiState = MutableStateFlow(
        PanelUiState(
            aimbotEnabled = initialAimbot,
            locationEspEnabled = initialEsp,
            isOverlayRunning = OverlayPanelStateHolder.state.value.isOverlayRunning,
            bannerKind = steadyBannerFor(initialAimbot, initialEsp)
        )
    )
    val uiState: StateFlow<PanelUiState> = _uiState.asStateFlow()

    private var bannerResetJob: Job? = null

    init {
        viewModelScope.launch {
            OverlayPanelStateHolder.state.collect { shared ->
                savedStateHandle[KEY_AIMBOT] = shared.aimbotEnabled
                savedStateHandle[KEY_ESP] = shared.locationEspEnabled
                _uiState.update { current ->
                    val aimbotChanged = current.aimbotEnabled != shared.aimbotEnabled
                    val espChanged = current.locationEspEnabled != shared.locationEspEnabled
                    val overlayChanged = current.isOverlayRunning != shared.isOverlayRunning

                    val updatedBanner = when {
                        aimbotChanged -> if (shared.aimbotEnabled) PanelBannerKind.AIMBOT_ON else PanelBannerKind.AIMBOT_OFF
                        espChanged -> if (shared.locationEspEnabled) PanelBannerKind.ESP_ON else PanelBannerKind.ESP_OFF
                        overlayChanged -> if (shared.isOverlayRunning) PanelBannerKind.OVERLAY_STARTED else PanelBannerKind.OVERLAY_STOPPED
                        else -> current.bannerKind
                    }

                    val positive = when {
                        aimbotChanged -> shared.aimbotEnabled
                        espChanged -> shared.locationEspEnabled
                        overlayChanged -> shared.isOverlayRunning
                        else -> current.isPositiveActivation
                    }

                    if (aimbotChanged || espChanged || overlayChanged) {
                        scheduleSteadyBanner()
                    }

                    current.copy(
                        aimbotEnabled = shared.aimbotEnabled,
                        locationEspEnabled = shared.locationEspEnabled,
                        isOverlayRunning = shared.isOverlayRunning,
                        bannerKind = updatedBanner,
                        isPositiveActivation = positive,
                        sweepTriggerId = if ((aimbotChanged && shared.aimbotEnabled) ||
                            (espChanged && shared.locationEspEnabled) ||
                            (overlayChanged && shared.isOverlayRunning)
                        ) current.sweepTriggerId + 1 else current.sweepTriggerId,
                        aimbotPopTrigger = if (aimbotChanged) current.aimbotPopTrigger + 1 else current.aimbotPopTrigger,
                        espPopTrigger = if (espChanged) current.espPopTrigger + 1 else current.espPopTrigger
                    )
                }
            }
        }
    }

    fun updateOverlayPermission(granted: Boolean) {
        _uiState.update { it.copy(hasOverlayPermission = granted) }
    }

    fun toggleAimbot() {
        val next = OverlayPanelStateHolder.toggleAimbot()
        savedStateHandle[KEY_AIMBOT] = next
    }

    fun toggleLocationEsp() {
        val next = OverlayPanelStateHolder.toggleLocationEsp()
        savedStateHandle[KEY_ESP] = next
    }

    fun resetPanel() {
        OverlayPanelStateHolder.resetToggles()
        savedStateHandle[KEY_AIMBOT] = false
        savedStateHandle[KEY_ESP] = false
        _uiState.update { state ->
            state.copy(
                aimbotEnabled = false,
                locationEspEnabled = false,
                bannerKind = PanelBannerKind.RESET_COMPLETE,
                isPositiveActivation = false,
                aimbotPopTrigger = state.aimbotPopTrigger + 1,
                espPopTrigger = state.espPopTrigger + 1,
                resetSpinTrigger = state.resetSpinTrigger + 1
            )
        }
        scheduleSteadyBanner()
    }

    private fun scheduleSteadyBanner() {
        bannerResetJob?.cancel()
        bannerResetJob = viewModelScope.launch {
            delay(1250L)
            _uiState.update { state ->
                state.copy(
                    bannerKind = steadyBannerFor(state.aimbotEnabled, state.locationEspEnabled),
                    isPositiveActivation = false
                )
            }
        }
    }

    private fun steadyBannerFor(aimbot: Boolean, esp: Boolean): PanelBannerKind {
        val activeCount = (if (aimbot) 1 else 0) + (if (esp) 1 else 0)
        return when (activeCount) {
            2 -> PanelBannerKind.TWO_ACTIVE
            1 -> PanelBannerKind.ONE_ACTIVE
            else -> PanelBannerKind.STANDBY
        }
    }

    companion object {
        private const val KEY_AIMBOT = "aimbot_enabled"
        private const val KEY_ESP = "location_esp_enabled"
    }
}
