// Copyright (C) 2024 MyOEM
// SPDX-License-Identifier: Apache-2.0

#define LOG_TAG "VtsThermalControlServiceTest"

#include <cmath>
#include <thread>
#include <vector>
#include <atomic>
#include <chrono>

#include <gtest/gtest.h>

#include <android/binder_manager.h>
#include <android/binder_process.h>
#include <aidl/com/myoem/thermalcontrol/IThermalControlService.h>

using aidl::com::myoem::thermalcontrol::IThermalControlService;

static constexpr const char* kServiceName =
    "com.myoem.thermalcontrol.IThermalControlService/default";

static constexpr float kMinReasonableTemp =   0.0f;
static constexpr float kMaxReasonableTemp = 120.0f;

class ThermalControlServiceTest : public ::testing::Test {
  public:
    static std::shared_ptr<IThermalControlService> sService;

    static void SetUpTestSuite() {
        ABinderProcess_setThreadPoolMaxThreadCount(0);
        ABinderProcess_startThreadPool();

        ndk::SpAIBinder binder(AServiceManager_waitForService(kServiceName));
        if (binder.get() != nullptr) {
            sService = IThermalControlService::fromBinder(binder);
        }
    }

  protected:
    void SetUp() override {
        if (sService == nullptr) {
            GTEST_SKIP() << "thermalcontrold not running";
        }
    }
};

std::shared_ptr<IThermalControlService> ThermalControlServiceTest::sService = nullptr;

#define ASSERT_BINDER_OK(status) \
    ASSERT_TRUE((status).isOk()) << "Binder call failed: " << (status).getDescription()

#define ASSERT_SERVICE_ERROR(status, expectedCode)                        \
    do {                                                                  \
        ASSERT_FALSE((status).isOk());                                    \
        ASSERT_EQ((status).getExceptionCode(), EX_SERVICE_SPECIFIC);      \
        EXPECT_EQ((status).getServiceSpecificError(), (expectedCode));    \
    } while (0)

TEST_F(ThermalControlServiceTest, VintfManifest_ServiceDeclared) {
    ASSERT_TRUE(AServiceManager_isDeclared(kServiceName))
        << "Service not declared in VINTF manifest: " << kServiceName;
}

TEST_F(ThermalControlServiceTest, ServiceAvailable_Running) {
    ndk::SpAIBinder binder(AServiceManager_checkService(kServiceName));
    ASSERT_NE(binder.get(), nullptr);
}

TEST_F(ThermalControlServiceTest, ServiceProxy_ValidInterface) {
    ASSERT_NE(sService, nullptr);
}

TEST_F(ThermalControlServiceTest, GetCpuTemp_DoesNotCrash) {
    float result = 0.0f;
    ndk::ScopedAStatus status = sService->getCpuTemperatureCelsius(&result);

    bool acceptable = status.isOk() ||
        (status.getExceptionCode() == EX_SERVICE_SPECIFIC &&
         status.getServiceSpecificError() == IThermalControlService::ERROR_HAL_UNAVAILABLE);

    EXPECT_TRUE(acceptable) << status.getDescription();
}

TEST_F(ThermalControlServiceTest, GetCpuTemp_WhenOk_IsFiniteFloat) {
    float result = 0.0f;
    ndk::ScopedAStatus status = sService->getCpuTemperatureCelsius(&result);
    if (!status.isOk()) GTEST_SKIP() << "HAL unavailable";

    EXPECT_TRUE(std::isfinite(result));
    EXPECT_FALSE(std::isnan(result));
}

TEST_F(ThermalControlServiceTest, GetCpuTemp_WhenOk_ReasonableRange) {
    float result = 0.0f;
    ndk::ScopedAStatus status = sService->getCpuTemperatureCelsius(&result);
    if (!status.isOk()) GTEST_SKIP() << "HAL unavailable";

    EXPECT_GE(result, kMinReasonableTemp);
    EXPECT_LE(result, kMaxReasonableTemp);
}

TEST_F(ThermalControlServiceTest, GetFanSpeedRpm_IsMinusOneOrPositive) {
    int32_t result = 0;
    ASSERT_BINDER_OK(sService->getFanSpeedRpm(&result));
    EXPECT_TRUE(result == -1 || result >= 0);
}

TEST_F(ThermalControlServiceTest, GetFanSpeedPercent_InRange_0To100) {
    int32_t result = 0;
    ASSERT_BINDER_OK(sService->getFanSpeedPercent(&result));
    EXPECT_GE(result, 0);
    EXPECT_LE(result, 100);
}

TEST_F(ThermalControlServiceTest, IsFanRunning_ReturnsBool) {
    bool result = false;
    ASSERT_BINDER_OK(sService->isFanRunning(&result));
}

TEST_F(ThermalControlServiceTest, IsFanAutoMode_ReturnsBool) {
    bool result = false;
    ASSERT_BINDER_OK(sService->isFanAutoMode(&result));
}

TEST_F(ThermalControlServiceTest, IsFanRunning_ConsistentWithFanSpeedPercent) {
    int32_t percent = 0;
    bool running    = false;
    ASSERT_BINDER_OK(sService->getFanSpeedPercent(&percent));
    ASSERT_BINDER_OK(sService->isFanRunning(&running));
    if (percent > 0) EXPECT_TRUE(running);
}

TEST_F(ThermalControlServiceTest, SetFanAutoMode_True_Succeeds) {
    ndk::ScopedAStatus status = sService->setFanAutoMode(true);
    bool acceptable = status.isOk() ||
        (status.getExceptionCode() == EX_SERVICE_SPECIFIC &&
         status.getServiceSpecificError() == IThermalControlService::ERROR_SYSFS_WRITE);
    EXPECT_TRUE(acceptable);
}

TEST_F(ThermalControlServiceTest, SetFanAutoMode_True_ReflectedInGetter) {
    ndk::ScopedAStatus writeStatus = sService->setFanAutoMode(true);
    if (!writeStatus.isOk()) GTEST_SKIP() << "Hardware not available";

    bool autoMode = false;
    ASSERT_BINDER_OK(sService->isFanAutoMode(&autoMode));
    EXPECT_TRUE(autoMode);
}

TEST_F(ThermalControlServiceTest, SetFanAutoMode_False_ReflectedInGetter) {
    ndk::ScopedAStatus writeStatus = sService->setFanAutoMode(false);
    if (!writeStatus.isOk()) GTEST_SKIP() << "Hardware not available";

    bool autoMode = true;
    ASSERT_BINDER_OK(sService->isFanAutoMode(&autoMode));
    EXPECT_FALSE(autoMode);

    sService->setFanAutoMode(true);
}

TEST_F(ThermalControlServiceTest, SetFanEnabled_True_ExitsAutoMode) {
    ndk::ScopedAStatus autoStatus = sService->setFanAutoMode(true);
    if (!autoStatus.isOk()) GTEST_SKIP() << "Hardware not available";

    ndk::ScopedAStatus enableStatus = sService->setFanEnabled(true);
    if (!enableStatus.isOk()) GTEST_SKIP() << "setFanEnabled failed";

    bool autoMode = true;
    ASSERT_BINDER_OK(sService->isFanAutoMode(&autoMode));
    EXPECT_FALSE(autoMode);

    sService->setFanAutoMode(true);
}

TEST_F(ThermalControlServiceTest, SetFanSpeed_50_ReflectedInGetter) {
    ndk::ScopedAStatus writeStatus = sService->setFanSpeed(50);
    if (!writeStatus.isOk()) GTEST_SKIP() << "Hardware not available";

    int32_t percent = -1;
    ASSERT_BINDER_OK(sService->getFanSpeedPercent(&percent));
    EXPECT_NEAR(percent, 50, 1);

    sService->setFanAutoMode(true);
}

TEST_F(ThermalControlServiceTest, SetFanSpeed_Negative_ThrowsCode2) {
    ndk::ScopedAStatus status = sService->setFanSpeed(-1);
    ASSERT_SERVICE_ERROR(status, IThermalControlService::ERROR_INVALID_SPEED);
}

TEST_F(ThermalControlServiceTest, SetFanSpeed_Over100_ThrowsCode2) {
    ndk::ScopedAStatus status = sService->setFanSpeed(101);
    ASSERT_SERVICE_ERROR(status, IThermalControlService::ERROR_INVALID_SPEED);
}

TEST_F(ThermalControlServiceTest, SetFanSpeed_200_ThrowsCode2) {
    ndk::ScopedAStatus status = sService->setFanSpeed(200);
    ASSERT_SERVICE_ERROR(status, IThermalControlService::ERROR_INVALID_SPEED);
}

TEST_F(ThermalControlServiceTest, ErrorCode_Values_Stable) {
    EXPECT_EQ(IThermalControlService::ERROR_HAL_UNAVAILABLE, 1);
    EXPECT_EQ(IThermalControlService::ERROR_INVALID_SPEED,   2);
    EXPECT_EQ(IThermalControlService::ERROR_SYSFS_WRITE,     3);
}

TEST_F(ThermalControlServiceTest, ConcurrentReads_NoCrash) {
    static constexpr int kThreads        = 8;
    static constexpr int kCallsPerThread = 50;

    std::atomic<int> failures{0};
    std::vector<std::thread> threads;
    threads.reserve(kThreads);

    for (int t = 0; t < kThreads; ++t) {
        threads.emplace_back([&]() {
            for (int i = 0; i < kCallsPerThread; ++i) {
                float temp = 0.0f;
                ndk::ScopedAStatus s = sService->getCpuTemperatureCelsius(&temp);
                bool ok = s.isOk() ||
                    (s.getExceptionCode() == EX_SERVICE_SPECIFIC &&
                     s.getServiceSpecificError() == IThermalControlService::ERROR_HAL_UNAVAILABLE);
                if (!ok) failures++;
            }
        });
    }

    for (auto& th : threads) th.join();
    EXPECT_EQ(failures.load(), 0);
}

TEST_F(ThermalControlServiceTest, RepeatedReads_NoCrash) {
    for (int i = 0; i < 100; ++i) {
        float temp = 0.0f;
        ndk::ScopedAStatus s = sService->getCpuTemperatureCelsius(&temp);
        bool acceptable = s.isOk() ||
            (s.getExceptionCode() == EX_SERVICE_SPECIFIC &&
             s.getServiceSpecificError() == IThermalControlService::ERROR_HAL_UNAVAILABLE);
        ASSERT_TRUE(acceptable) << "Call " << i << " failed: " << s.getDescription();
    }
}
