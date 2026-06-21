// Copyright (C) 2024 MyOEM
// SPDX-License-Identifier: Apache-2.0

package com.myoem.thermalmonitor

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import com.myoem.thermalcontrol.ThermalControlManager
import com.myoem.thermalmonitor.ui.ThermalScreen
import com.myoem.thermalmonitor.ui.theme.ThermalMonitorTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ThermalScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private class FakeThermalViewModel : ThermalViewModel(ThermalControlManager(null)) {
        private val _fakeState = MutableStateFlow(UiState())
        override val uiState get() = _fakeState

        fun setState(state: UiState) { _fakeState.value = state }

        var lastSetFanSpeed: Int? = null
        var turnFanOnCalled    = false
        var turnFanOffCalled   = false
        var setAutoModeCalled  = false

        override fun turnFanOn()                 { turnFanOnCalled = true }
        override fun turnFanOff()                { turnFanOffCalled = true }
        override fun setFanSpeed(percent: Int)   { lastSetFanSpeed = percent }
        override fun setAutoMode()               { setAutoModeCalled = true }
    }

    private fun launchScreen(state: UiState): FakeThermalViewModel {
        val viewModel = FakeThermalViewModel()
        viewModel.setState(state)
        composeTestRule.setContent {
            ThermalMonitorTheme { ThermalScreen(viewModel = viewModel) }
        }
        return viewModel
    }

    @Test
    fun whenServiceUnavailable_ShowsServiceUnavailableTitle() {
        launchScreen(UiState(serviceAvailable = false))
        composeTestRule.onNodeWithText("Service Unavailable", substring = true).assertIsDisplayed()
    }

    @Test
    fun whenServiceUnavailable_ShowsErrorMessageDetail() {
        launchScreen(UiState(serviceAvailable = false, errorMessage = "thermalcontrold not running"))
        composeTestRule.onNodeWithText("thermalcontrold not running", substring = true).assertIsDisplayed()
    }

    @Test
    fun whenServiceAvailable_ShowsTemperatureValue() {
        launchScreen(UiState(serviceAvailable = true, cpuTempCelsius = 42.5f))
        composeTestRule.onNodeWithText("42.5 °C", substring = true).assertIsDisplayed()
    }

    @Test
    fun whenServiceAvailable_ShowsTemperatureCategory_Cool() {
        launchScreen(UiState(serviceAvailable = true, cpuTempCelsius = 42.5f))
        composeTestRule.onNodeWithText("Cool", substring = true).assertIsDisplayed()
    }

    @Test
    fun whenServiceAvailable_ShowsTemperatureCategory_Hot() {
        launchScreen(UiState(serviceAvailable = true, cpuTempCelsius = 75.0f))
        composeTestRule.onNodeWithText("Hot", substring = true).assertIsDisplayed()
    }

    @Test
    fun whenServiceAvailable_ShowsFanSpeedPercent() {
        launchScreen(UiState(serviceAvailable = true, fanSpeedPercent = 50))
        composeTestRule.onNodeWithText("50%", substring = true).assertIsDisplayed()
    }

    @Test
    fun whenServiceAvailable_ShowsFanRpm() {
        launchScreen(UiState(serviceAvailable = true, fanRpm = 1200))
        composeTestRule.onNodeWithText("1200 RPM", substring = true).assertIsDisplayed()
    }

    @Test
    fun whenServiceAvailable_FanRpmUnavailable_ShowsNA() {
        launchScreen(UiState(serviceAvailable = true, fanRpm = -1))
        composeTestRule.onNodeWithText("N/A", substring = true).assertIsDisplayed()
    }

    @Test
    fun whenFanRunning_ShowsYes() {
        launchScreen(UiState(serviceAvailable = true, isFanRunning = true))
        composeTestRule.onNodeWithText("Yes", substring = true).assertIsDisplayed()
    }

    @Test
    fun whenFanNotRunning_ShowsNo() {
        launchScreen(UiState(serviceAvailable = true, isFanRunning = false))
        composeTestRule.onNodeWithText("No", substring = true).assertIsDisplayed()
    }

    @Test
    fun fanControlCard_TurnOnButton_WhenClicked_CallsViewModel() {
        val vm = launchScreen(UiState(serviceAvailable = true))
        composeTestRule.onNodeWithText("Turn On", substring = true).performClick()
        assertTrue(vm.turnFanOnCalled)
    }

    @Test
    fun fanControlCard_TurnOffButton_WhenClicked_CallsViewModel() {
        val vm = launchScreen(UiState(serviceAvailable = true))
        composeTestRule.onNodeWithText("Turn Off", substring = true).performClick()
        assertTrue(vm.turnFanOffCalled)
    }

    @Test
    fun fanControlCard_AutoButton_WhenClicked_CallsViewModel() {
        val vm = launchScreen(UiState(serviceAvailable = true))
        composeTestRule.onNodeWithText("Auto", substring = true).performClick()
        assertTrue(vm.setAutoModeCalled)
    }

    @Test
    fun fanControlCard_ApplyButton_WhenClicked_CallsViewModelWithCurrentPercent() {
        val vm = launchScreen(UiState(serviceAvailable = true, fanSpeedPercent = 50))
        composeTestRule.onNodeWithText("Apply", substring = true).performClick()
        assertEquals(50, vm.lastSetFanSpeed)
    }

    @Test
    fun autoModeBadge_WhenAutoModeTrue_IsDisplayed() {
        launchScreen(UiState(serviceAvailable = true, isAutoMode = true))
        composeTestRule.onNodeWithText("AUTO").assertIsDisplayed()
    }

    @Test
    fun autoModeBadge_WhenAutoModeFalse_IsNotDisplayed() {
        launchScreen(UiState(serviceAvailable = true, isAutoMode = false))
        composeTestRule.onAllNodesWithText("AUTO").assertCountEquals(0)
    }

    @Test
    fun stateUpdate_NewTemp_UIRefreshes() {
        val viewModel = FakeThermalViewModel()
        composeTestRule.setContent {
            ThermalMonitorTheme { ThermalScreen(viewModel = viewModel) }
        }

        viewModel.setState(UiState(serviceAvailable = true, cpuTempCelsius = 40.0f))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("40.0 °C", substring = true).assertIsDisplayed()

        viewModel.setState(UiState(serviceAvailable = true, cpuTempCelsius = 90.0f))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("90.0 °C", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Critical", substring = true).assertIsDisplayed()
    }

    @Test
    fun stateUpdate_ServiceBecomesAvailable_ErrorCardHides() {
        val viewModel = FakeThermalViewModel()
        composeTestRule.setContent {
            ThermalMonitorTheme { ThermalScreen(viewModel = viewModel) }
        }

        viewModel.setState(UiState(serviceAvailable = false, errorMessage = "thermalcontrold not running"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Service Unavailable", substring = true).assertIsDisplayed()

        viewModel.setState(UiState(serviceAvailable = true, cpuTempCelsius = 45.0f))
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText("Service Unavailable", substring = true).assertCountEquals(0)
        composeTestRule.onNodeWithText("45.0 °C", substring = true).assertIsDisplayed()
    }
}
