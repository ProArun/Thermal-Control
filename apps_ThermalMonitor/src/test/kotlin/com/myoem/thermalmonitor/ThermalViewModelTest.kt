// Copyright (C) 2024 MyOEM
// SPDX-License-Identifier: Apache-2.0

package com.myoem.thermalmonitor

import com.myoem.thermalcontrol.ThermalControlManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ThermalViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() { Dispatchers.setMain(testDispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun initialState_ServiceUnavailable_ServiceAvailableFalse() = runTest {
        val viewModel = ThermalViewModel(ThermalControlManager(null))
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.serviceAvailable)
    }

    @Test
    fun initialState_ServiceUnavailable_CpuTempIsZero() = runTest {
        val viewModel = ThermalViewModel(ThermalControlManager(null))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(0f, viewModel.uiState.value.cpuTempCelsius, 0f)
    }

    @Test
    fun initialState_ServiceUnavailable_FanRpmIsMinusOne() = runTest {
        val viewModel = ThermalViewModel(ThermalControlManager(null))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(-1, viewModel.uiState.value.fanRpm)
    }

    @Test
    fun initialState_ServiceUnavailable_IsAutoModeTrue() = runTest {
        val viewModel = ThermalViewModel(ThermalControlManager(null))
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isAutoMode)
    }

    @Test
    fun initialState_ServiceUnavailable_ErrorMessageIsSet() = runTest {
        val viewModel = ThermalViewModel(ThermalControlManager(null))
        testDispatcher.scheduler.advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.errorMessage!!.isNotEmpty())
    }

    private fun mockAvailableManager(
        temp: Float = 42.5f,
        rpm: Int = 1200,
        percent: Int = 50,
        isRunning: Boolean = true,
        isAuto: Boolean = false
    ): ThermalControlManager {
        val mock = mock(ThermalControlManager::class.java)
        whenever(mock.isAvailable()).thenReturn(true)
        whenever(mock.getCpuTemperatureCelsius()).thenReturn(temp)
        whenever(mock.getFanSpeedRpm()).thenReturn(rpm)
        whenever(mock.getFanSpeedPercent()).thenReturn(percent)
        whenever(mock.isFanRunning()).thenReturn(isRunning)
        whenever(mock.isFanAutoMode()).thenReturn(isAuto)
        whenever(mock.setFanSpeed(org.mockito.ArgumentMatchers.anyInt())).thenReturn(true)
        return mock
    }

    @Test
    fun fetchData_WhenAvailable_UpdatesCpuTemp() = runTest {
        val viewModel = ThermalViewModel(mockAvailableManager(temp = 42.5f))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(42.5f, viewModel.uiState.value.cpuTempCelsius, 0.01f)
    }

    @Test
    fun fetchData_WhenAvailable_UpdatesFanRpm() = runTest {
        val viewModel = ThermalViewModel(mockAvailableManager(rpm = 1200))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1200, viewModel.uiState.value.fanRpm)
    }

    @Test
    fun fetchData_WhenAvailable_UpdatesFanSpeedPercent() = runTest {
        val viewModel = ThermalViewModel(mockAvailableManager(percent = 75))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(75, viewModel.uiState.value.fanSpeedPercent)
    }

    @Test
    fun fetchData_WhenAvailable_SetsServiceAvailableTrue() = runTest {
        val viewModel = ThermalViewModel(mockAvailableManager())
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.serviceAvailable)
    }

    @Test
    fun fetchData_WhenAvailable_ClearsErrorMessage() = runTest {
        val viewModel = ThermalViewModel(mockAvailableManager())
        testDispatcher.scheduler.advanceUntilIdle()
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun turnFanOn_CallsManagerSetFanEnabled_True() = runTest {
        val manager = mockAvailableManager()
        val viewModel = ThermalViewModel(manager)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.turnFanOn()
        testDispatcher.scheduler.advanceUntilIdle()
        verify(manager).setFanEnabled(true)
    }

    @Test
    fun turnFanOff_CallsManagerSetFanEnabled_False() = runTest {
        val manager = mockAvailableManager()
        val viewModel = ThermalViewModel(manager)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.turnFanOff()
        testDispatcher.scheduler.advanceUntilIdle()
        verify(manager).setFanEnabled(false)
    }

    @Test
    fun setFanSpeed_50_CallsManagerWithPercent50() = runTest {
        val manager = mockAvailableManager()
        val viewModel = ThermalViewModel(manager)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.setFanSpeed(50)
        testDispatcher.scheduler.advanceUntilIdle()
        verify(manager).setFanSpeed(50)
    }

    @Test
    fun setAutoMode_CallsManagerSetFanAutoMode_True() = runTest {
        val manager = mockAvailableManager()
        val viewModel = ThermalViewModel(manager)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.setAutoMode()
        testDispatcher.scheduler.advanceUntilIdle()
        verify(manager).setFanAutoMode(true)
    }

    @Test
    fun turnFanOn_ThenFetchData_StateUpdated() = runTest {
        val manager = mockAvailableManager(percent = 0, isRunning = false)
        val viewModel = ThermalViewModel(manager)
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isFanRunning)

        whenever(manager.getFanSpeedPercent()).thenReturn(100)
        whenever(manager.isFanRunning()).thenReturn(true)

        viewModel.turnFanOn()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isFanRunning)
    }

    @Test
    fun autoRefresh_After2000ms_StateUpdatesAgain() = runTest {
        val manager = mockAvailableManager(temp = 40.0f)
        val viewModel = ThermalViewModel(manager)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(40.0f, viewModel.uiState.value.cpuTempCelsius, 0.01f)

        whenever(manager.getCpuTemperatureCelsius()).thenReturn(55.0f)
        advanceTimeBy(2001L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(55.0f, viewModel.uiState.value.cpuTempCelsius, 0.01f)
    }
}
