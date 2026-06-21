// Copyright (C) 2024 MyOEM
// SPDX-License-Identifier: Apache-2.0

#pragma once

#include <thermalcontrol/IThermalControlHal.h>

#include <cstdint>
#include <string>

namespace myoem::thermalcontrol {

class ThermalControlHal : public IThermalControlHal {
public:
    ThermalControlHal();

    float   getCpuTemperatureCelsius() override;
    int32_t getFanSpeedRpm()           override;
    int32_t getFanSpeedPercent()       override;
    bool    isFanRunning()             override;
    bool    isAutoMode()               override;

    bool setFanEnabled(bool enabled)    override;
    bool setFanSpeed(int32_t percent)   override;
    bool setAutoMode(bool autoMode)     override;

private:
    std::string mHwmonPath;
    bool        mAvailable;
};

}  // namespace myoem::thermalcontrol
