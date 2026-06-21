#define LOG_TAG "thermalcontrold"

// Copyright (C) 2024 MyOEM
// SPDX-License-Identifier: Apache-2.0

#include <android/binder_manager.h>
#include <android/binder_process.h>
#include <log/log.h>

#include "ThermalControlService.h"

static constexpr const char* kServiceName =
        "com.myoem.thermalcontrol.IThermalControlService/default";

int main() {
    ALOGI("thermalcontrold starting");

    ABinderProcess_setThreadPoolMaxThreadCount(4);
    ABinderProcess_startThreadPool();

    auto service = ndk::SharedRefBase::make<
            aidl::com::myoem::thermalcontrol::ThermalControlService>();

    binder_status_t status = AServiceManager_addService(
            service->asBinder().get(), kServiceName);

    if (status != STATUS_OK) {
        ALOGE("addService('%s') failed: %d", kServiceName, status);
        return 1;
    }

    ALOGI("thermalcontrold registered as '%s'", kServiceName);
    ABinderProcess_joinThreadPool();
    return 0;
}
