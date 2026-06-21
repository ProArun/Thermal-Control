
#pragma once

#include <cstdint>
#include <memory>

namespace myoem::thermalcontrol {

class IThermalControlHal {
public:
    virtual ~IThermalControlHal() = default;

    virtual float   getCpuTemperatureCelsius() = 0;
    virtual int32_t getFanSpeedRpm()           = 0;
    virtual int32_t getFanSpeedPercent()       = 0;
    virtual bool    isFanRunning()             = 0;
    virtual bool    isAutoMode()               = 0;

    virtual bool setFanEnabled(bool enabled)    = 0;
    virtual bool setFanSpeed(int32_t percent)   = 0;
    virtual bool setAutoMode(bool autoMode)     = 0;
};

std::unique_ptr<IThermalControlHal> createThermalControlHal();

}  // namespace myoem::thermalcontrol
