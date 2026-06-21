// Copyright (C) 2024 MyOEM
// SPDX-License-Identifier: Apache-2.0

#pragma once

#include <aidl/com/myoem/thermalcontrol/BnThermalControlService.h>
#include <thermalcontrol/IThermalControlHal.h>

#include <memory>

namespace aidl::com::myoem::thermalcontrol {

class ThermalControlService : public BnThermalControlService {
public:
    ThermalControlService();

    ndk::ScopedAStatus getCpuTemperatureCelsius(float* _aidl_return) override;
    ndk::ScopedAStatus getFanSpeedRpm(int32_t* _aidl_return)         override;
    ndk::ScopedAStatus isFanRunning(bool* _aidl_return)              override;
    ndk::ScopedAStatus getFanSpeedPercent(int32_t* _aidl_return)     override;
    ndk::ScopedAStatus isFanAutoMode(bool* _aidl_return)             override;

    ndk::ScopedAStatus setFanEnabled(bool enabled)       override;
    ndk::ScopedAStatus setFanSpeed(int32_t speedPercent) override;
    ndk::ScopedAStatus setFanAutoMode(bool autoMode)     override;

private:
    std::unique_ptr<::myoem::thermalcontrol::IThermalControlHal> mHal;
};

}  // namespace aidl::com::myoem::thermalcontrol
