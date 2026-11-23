package com.example

import androidx.lifecycle.SavedStateHandle
import com.example.overlay.OverlayPanelStateHolder
import com.example.ui.panel.PanelBannerKind
import com.example.ui.panel.PanelViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PanelViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        OverlayPanelStateHolder.resetToggles()
        OverlayPanelStateHolder.setOverlayRunning(false)
    }

    @After
    fun tearDown() {
        OverlayPanelStateHolder.resetToggles()
        OverlayPanelStateHolder.setOverlayRunning(false)
        Dispatchers.resetMain()
    }

    @Test
    fun `toggling features syncs with overlay holder and resets cleanly`() = runTest(testDispatcher) {
        val savedStateHandle = SavedStateHandle()
        val viewModel = PanelViewModel(savedStateHandle)
        runCurrent()

        assertFalse(viewModel.uiState.value.aimbotEnabled)
        assertFalse(viewModel.uiState.value.locationEspEnabled)
        assertEquals(PanelBannerKind.STANDBY, viewModel.uiState.value.bannerKind)

        viewModel.toggleAimbot()
        runCurrent()
        assertTrue(viewModel.uiState.value.aimbotEnabled)
        assertTrue(OverlayPanelStateHolder.state.value.aimbotEnabled)
        assertEquals(PanelBannerKind.AIMBOT_ON, viewModel.uiState.value.bannerKind)

        // Simulate toggle from the floating overlay
        OverlayPanelStateHolder.toggleLocationEsp()
        runCurrent()
        assertTrue(viewModel.uiState.value.locationEspEnabled)
        assertEquals(PanelBannerKind.ESP_ON, viewModel.uiState.value.bannerKind)

        advanceTimeBy(1300L)
        runCurrent()
        assertEquals(PanelBannerKind.TWO_ACTIVE, viewModel.uiState.value.bannerKind)

        // Recreating ViewModel preserves session toggle states
        val restoredViewModel = PanelViewModel(savedStateHandle)
        runCurrent()
        assertTrue(restoredViewModel.uiState.value.aimbotEnabled)
        assertTrue(restoredViewModel.uiState.value.locationEspEnabled)
        assertEquals(PanelBannerKind.TWO_ACTIVE, restoredViewModel.uiState.value.bannerKind)

        restoredViewModel.resetPanel()
        runCurrent()
        assertFalse(restoredViewModel.uiState.value.aimbotEnabled)
        assertFalse(restoredViewModel.uiState.value.locationEspEnabled)
        assertFalse(OverlayPanelStateHolder.state.value.aimbotEnabled)
        assertFalse(OverlayPanelStateHolder.state.value.locationEspEnabled)
        assertEquals(PanelBannerKind.RESET_COMPLETE, restoredViewModel.uiState.value.bannerKind)
    }
}
