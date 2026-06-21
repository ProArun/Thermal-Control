// Copyright (C) 2024 MyOEM
// SPDX-License-Identifier: Apache-2.0

package com.myoem.thermalcontrol;

import android.os.IBinder;
import android.os.ServiceManager;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

@RunWith(AndroidJUnit4.class)
public class ThermalControlManagerTest {

    @Test
    public void constructor_NullBinder_IsAvailableFalse() {
        assertFalse(new ThermalControlManager(null).isAvailable());
    }

    @Test
    public void serviceNameConstant_ExactValue() {
        assertEquals(
            "com.myoem.thermalcontrol.IThermalControlService/default",
            ThermalControlManager.SERVICE_NAME
        );
    }

    @Test
    public void getCpuTemperatureCelsius_NullBinder_ReturnsZero() {
        assertEquals(0.0f, new ThermalControlManager(null).getCpuTemperatureCelsius(), 0.0f);
    }

    @Test
    public void getFanSpeedRpm_NullBinder_ReturnsMinusOne() {
        assertEquals(-1, new ThermalControlManager(null).getFanSpeedRpm());
    }

    @Test
    public void isFanRunning_NullBinder_ReturnsFalse() {
        assertFalse(new ThermalControlManager(null).isFanRunning());
    }

    @Test
    public void getFanSpeedPercent_NullBinder_ReturnsZero() {
        assertEquals(0, new ThermalControlManager(null).getFanSpeedPercent());
    }

    @Test
    public void isFanAutoMode_NullBinder_ReturnsTrueSafeDefault() {
        assertTrue(new ThermalControlManager(null).isFanAutoMode());
    }

    @Test
    public void setFanEnabled_NullBinder_NoException() {
        ThermalControlManager mgr = new ThermalControlManager(null);
        mgr.setFanEnabled(true);
        mgr.setFanEnabled(false);
    }

    @Test
    public void setFanSpeed_NullBinder_ReturnsFalse() {
        assertFalse(new ThermalControlManager(null).setFanSpeed(50));
    }

    @Test
    public void setFanAutoMode_NullBinder_NoException() {
        ThermalControlManager mgr = new ThermalControlManager(null);
        mgr.setFanAutoMode(true);
        mgr.setFanAutoMode(false);
    }

    @Test
    public void categorizeTemp_NegativeTemp_ReturnsCool() {
        assertEquals(ThermalControlManager.TEMP_COOL,
            ThermalControlManager.categorizeTemperature(-10.0f));
    }

    @Test
    public void categorizeTemp_Zero_ReturnsCool() {
        assertEquals(ThermalControlManager.TEMP_COOL,
            ThermalControlManager.categorizeTemperature(0.0f));
    }

    @Test
    public void categorizeTemp_MidCool_ReturnsCool() {
        assertEquals(ThermalControlManager.TEMP_COOL,
            ThermalControlManager.categorizeTemperature(30.0f));
    }

    @Test
    public void categorizeTemp_JustBelow50_ReturnsCool() {
        assertEquals(ThermalControlManager.TEMP_COOL,
            ThermalControlManager.categorizeTemperature(49.9f));
    }

    @Test
    public void categorizeTemp_Boundary50_ReturnsWarm() {
        assertEquals(ThermalControlManager.TEMP_WARM,
            ThermalControlManager.categorizeTemperature(50.0f));
    }

    @Test
    public void categorizeTemp_MidWarm_ReturnsWarm() {
        assertEquals(ThermalControlManager.TEMP_WARM,
            ThermalControlManager.categorizeTemperature(60.0f));
    }

    @Test
    public void categorizeTemp_Boundary70_ReturnsHot() {
        assertEquals(ThermalControlManager.TEMP_HOT,
            ThermalControlManager.categorizeTemperature(70.0f));
    }

    @Test
    public void categorizeTemp_MidHot_ReturnsHot() {
        assertEquals(ThermalControlManager.TEMP_HOT,
            ThermalControlManager.categorizeTemperature(77.0f));
    }

    @Test
    public void categorizeTemp_Boundary85_ReturnsCritical() {
        assertEquals(ThermalControlManager.TEMP_CRITICAL,
            ThermalControlManager.categorizeTemperature(85.0f));
    }

    @Test
    public void categorizeTemp_VeryHigh_ReturnsCritical() {
        assertEquals(ThermalControlManager.TEMP_CRITICAL,
            ThermalControlManager.categorizeTemperature(120.0f));
    }

    @Test
    public void temperatureColor_CoolTemp_ReturnsGreen() {
        assertEquals(0xFF4CAF50, ThermalControlManager.temperatureColor(30.0f));
    }

    @Test
    public void temperatureColor_Boundary50_ReturnsAmber() {
        assertEquals(0xFFFFC107, ThermalControlManager.temperatureColor(50.0f));
    }

    @Test
    public void temperatureColor_WarmTemp_ReturnsAmber() {
        assertEquals(0xFFFFC107, ThermalControlManager.temperatureColor(60.0f));
    }

    @Test
    public void temperatureColor_Boundary70_ReturnsDeepOrange() {
        assertEquals(0xFFFF5722, ThermalControlManager.temperatureColor(70.0f));
    }

    @Test
    public void temperatureColor_Boundary85_ReturnsRed() {
        assertEquals(0xFFF44336, ThermalControlManager.temperatureColor(85.0f));
    }

    @Test
    public void temperatureColor_CriticalTemp_ReturnsRed() {
        assertEquals(0xFFF44336, ThermalControlManager.temperatureColor(90.0f));
    }

    @Test
    public void errorCode_HAL_UNAVAILABLE_Is1() {
        assertEquals(1, ThermalControlManager.ERROR_HAL_UNAVAILABLE);
    }

    @Test
    public void errorCode_INVALID_SPEED_Is2() {
        assertEquals(2, ThermalControlManager.ERROR_INVALID_SPEED);
    }

    @Test
    public void errorCode_SYSFS_WRITE_Is3() {
        assertEquals(3, ThermalControlManager.ERROR_SYSFS_WRITE);
    }

    private ThermalControlManager getLiveManager() {
        IBinder binder = ServiceManager.checkService(ThermalControlManager.SERVICE_NAME);
        return new ThermalControlManager(binder);
    }

    @Test
    public void live_IsAvailable_WhenServiceRunning() {
        ThermalControlManager mgr = getLiveManager();
        assumeTrue("thermalcontrold not running", mgr.isAvailable());
        assertTrue(mgr.isAvailable());
    }

    @Test
    public void live_GetCpuTemp_ReturnsPositive() {
        ThermalControlManager mgr = getLiveManager();
        assumeTrue("thermalcontrold not running", mgr.isAvailable());
        assertTrue(mgr.getCpuTemperatureCelsius() > 0.0f);
    }

    @Test
    public void live_SetFanSpeed_50_ReadBackEquals50() {
        ThermalControlManager mgr = getLiveManager();
        assumeTrue("thermalcontrold not running", mgr.isAvailable());
        boolean set = mgr.setFanSpeed(50);
        assumeTrue("Fan hardware not available", set);
        assertTrue(Math.abs(mgr.getFanSpeedPercent() - 50) <= 1);
        mgr.setFanAutoMode(true);
    }

    @Test
    public void live_ClientSideValidation_Negative_ReturnsFalse() {
        ThermalControlManager mgr = getLiveManager();
        assumeTrue("thermalcontrold not running", mgr.isAvailable());
        assertFalse(mgr.setFanSpeed(-1));
    }

    @Test
    public void live_ClientSideValidation_Over100_ReturnsFalse() {
        ThermalControlManager mgr = getLiveManager();
        assumeTrue("thermalcontrold not running", mgr.isAvailable());
        assertFalse(mgr.setFanSpeed(101));
    }
}
