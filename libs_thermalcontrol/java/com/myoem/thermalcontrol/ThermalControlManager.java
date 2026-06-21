
package com.myoem.thermalcontrol;

import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class ThermalControlManager {

    private static final String TAG = "ThermalControlManager";

    public static final String SERVICE_NAME =
            "com.myoem.thermalcontrol.IThermalControlService/default";

    public static final int ERROR_HAL_UNAVAILABLE = 1;
    public static final int ERROR_INVALID_SPEED   = 2;
    public static final int ERROR_SYSFS_WRITE     = 3;

    public static final String TEMP_COOL     = "Cool";
    public static final String TEMP_WARM     = "Warm";
    public static final String TEMP_HOT      = "Hot";
    public static final String TEMP_CRITICAL = "Critical";

    private final IThermalControlService mService;

    public ThermalControlManager(IBinder binder) {
        if (binder != null) {
            mService = IThermalControlService.Stub.asInterface(binder);
            Log.d(TAG, "Connected to " + SERVICE_NAME);
        } else {
            mService = null;
            Log.w(TAG, "Service not found: " + SERVICE_NAME);
        }
    }

    public boolean isAvailable() {
        return mService != null;
    }

    public float getCpuTemperatureCelsius() {
        if (mService == null) return 0.0f;
        try {
            return mService.getCpuTemperatureCelsius();
        } catch (RemoteException e) {
            Log.e(TAG, "getCpuTemperatureCelsius failed: " + e.getMessage());
            return 0.0f;
        }
    }

    public int getFanSpeedRpm() {
        if (mService == null) return -1;
        try {
            return mService.getFanSpeedRpm();
        } catch (RemoteException e) {
            Log.e(TAG, "getFanSpeedRpm failed: " + e.getMessage());
            return -1;
        }
    }

    public boolean isFanRunning() {
        if (mService == null) return false;
        try {
            return mService.isFanRunning();
        } catch (RemoteException e) {
            Log.e(TAG, "isFanRunning failed: " + e.getMessage());
            return false;
        }
    }

    public int getFanSpeedPercent() {
        if (mService == null) return 0;
        try {
            return mService.getFanSpeedPercent();
        } catch (RemoteException e) {
            Log.e(TAG, "getFanSpeedPercent failed: " + e.getMessage());
            return 0;
        }
    }

    public boolean isFanAutoMode() {
        if (mService == null) return true;
        try {
            return mService.isFanAutoMode();
        } catch (RemoteException e) {
            Log.e(TAG, "isFanAutoMode failed: " + e.getMessage());
            return true;
        }
    }

    public void setFanEnabled(boolean enabled) {
        if (mService == null) return;
        try {
            mService.setFanEnabled(enabled);
        } catch (RemoteException e) {
            Log.e(TAG, "setFanEnabled(" + enabled + ") failed: " + e.getMessage());
        }
    }

    public boolean setFanSpeed(int speedPercent) {
        if (mService == null) return false;
        if (speedPercent < 0 || speedPercent > 100) {
            Log.w(TAG, "setFanSpeed: invalid speedPercent=" + speedPercent);
            return false;
        }
        try {
            mService.setFanSpeed(speedPercent);
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "setFanSpeed(" + speedPercent + ") failed: " + e.getMessage());
            return false;
        }
    }

    public void setFanAutoMode(boolean autoMode) {
        if (mService == null) return;
        try {
            mService.setFanAutoMode(autoMode);
        } catch (RemoteException e) {
            Log.e(TAG, "setFanAutoMode(" + autoMode + ") failed: " + e.getMessage());
        }
    }

    public static String categorizeTemperature(float celsius) {
        if (celsius < 50.0f) return TEMP_COOL;
        if (celsius < 70.0f) return TEMP_WARM;
        if (celsius < 85.0f) return TEMP_HOT;
        return TEMP_CRITICAL;
    }

    public static int temperatureColor(float celsius) {
        if (celsius < 50.0f) return 0xFF4CAF50;
        if (celsius < 70.0f) return 0xFFFFC107;
        if (celsius < 85.0f) return 0xFFFF5722;
        return 0xFFF44336;
    }
}
