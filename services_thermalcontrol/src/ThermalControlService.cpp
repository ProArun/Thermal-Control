#define LOG_TAG "thermalcontrold"

// Copyright (C) 2024 MyOEM
// SPDX-License-Identifier: Apache-2.0

#include "ThermalControlService.h"

#include <log/log.h>
#include <thermalcontrol/IThermalControlHal.h>

namespace aidl::com::myoem::thermalcontrol {

ThermalControlService::ThermalControlService()
    : mHal(::myoem::thermalcontrol::createThermalControlHal()) {
    ALOGI("ThermalControlService created");
}

ndk::ScopedAStatus ThermalControlService::getCpuTemperatureCelsius(float* _aidl_return) {
    *_aidl_return = mHal->getCpuTemperatureCelsius();
    ALOGD("getCpuTemperatureCelsius() = %.1f°C", *_aidl_return);
    return ndk::ScopedAStatus::ok();
}

ndk::ScopedAStatus ThermalControlService::getFanSpeedRpm(int32_t* _aidl_return) {
    *_aidl_return = mHal->getFanSpeedRpm();
    ALOGD("getFanSpeedRpm() = %d", *_aidl_return);
    return ndk::ScopedAStatus::ok();
}

ndk::ScopedAStatus ThermalControlService::isFanRunning(bool* _aidl_return) {
    *_aidl_return = mHal->isFanRunning();
    ALOGD("isFanRunning() = %s", *_aidl_return ? "true" : "false");
    return ndk::ScopedAStatus::ok();
}

ndk::ScopedAStatus ThermalControlService::getFanSpeedPercent(int32_t* _aidl_return) {
    *_aidl_return = mHal->getFanSpeedPercent();
    ALOGD("getFanSpeedPercent() = %d%%", *_aidl_return);
    return ndk::ScopedAStatus::ok();
}

ndk::ScopedAStatus ThermalControlService::isFanAutoMode(bool* _aidl_return) {
    *_aidl_return = mHal->isAutoMode();
    ALOGD("isFanAutoMode() = %s", *_aidl_return ? "true" : "false");
    return ndk::ScopedAStatus::ok();
}

ndk::ScopedAStatus ThermalControlService::setFanEnabled(bool enabled) {
    ALOGD("setFanEnabled(%s)", enabled ? "true" : "false");
    if (!mHal->setFanEnabled(enabled)) {
        ALOGE("setFanEnabled: HAL write failed");
        return ndk::ScopedAStatus::fromServiceSpecificError(
                IThermalControlService::ERROR_SYSFS_WRITE);
    }
    return ndk::ScopedAStatus::ok();
}

ndk::ScopedAStatus ThermalControlService::setFanSpeed(int32_t speedPercent) {
    ALOGD("setFanSpeed(%d%%)", speedPercent);

    if (speedPercent < 0 || speedPercent > 100) {
        ALOGE("setFanSpeed: invalid speedPercent=%d", speedPercent);
        return ndk::ScopedAStatus::fromServiceSpecificError(
                IThermalControlService::ERROR_INVALID_SPEED);
    }

    if (!mHal->setFanSpeed(speedPercent)) {
        ALOGE("setFanSpeed: HAL write failed");
        return ndk::ScopedAStatus::fromServiceSpecificError(
                IThermalControlService::ERROR_SYSFS_WRITE);
    }
    return ndk::ScopedAStatus::ok();
}

ndk::ScopedAStatus ThermalControlService::setFanAutoMode(bool autoMode) {
    ALOGD("setFanAutoMode(%s)", autoMode ? "true" : "false");
    if (!mHal->setAutoMode(autoMode)) {
        ALOGE("setFanAutoMode: HAL write failed");
        return ndk::ScopedAStatus::fromServiceSpecificError(
                IThermalControlService::ERROR_SYSFS_WRITE);
    }
    return ndk::ScopedAStatus::ok();
}

}  // namespace aidl::com::myoem::thermalcontrol
