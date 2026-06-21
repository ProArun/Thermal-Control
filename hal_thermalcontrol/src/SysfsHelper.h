
#pragma once

#include <cstdint>
#include <string>

namespace myoem::thermalcontrol {

int         sysfsReadInt(const std::string& path, int defaultVal = -1);
float       sysfsReadFloat(const std::string& path, float defaultVal = -1.0f);
bool        sysfsWriteInt(const std::string& path, int value);
std::string discoverHwmonPath();

}  // namespace myoem::thermalcontrol
