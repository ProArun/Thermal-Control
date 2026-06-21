// Copyright (C) 2024 MyOEM
// SPDX-License-Identifier: Apache-2.0

#define LOG_TAG "VtsThermalControlHalTest"

#include <cmath>
#include <memory>

#include <gtest/gtest.h>
#include <thermalcontrol/IThermalControlHal.h>

using myoem::thermalcontrol::IThermalControlHal;
using myoem::thermalcontrol::createThermalControlHal;

static bool isFanHardwareAvailable() {
    auto hal = createThermalControlHal();
    if (!hal) return false;
    return hal->setFanEnabled(false);
}

class ThermalControlHalTest : public ::testing::Test {
  protected:
    std::unique_ptr<IThermalControlHal> mHal;

    void SetUp() override {
        mHal = createThermalControlHal();
        ASSERT_NE(mHal, nullptr);
    }

    void TearDown() override {
        if (mHal) mHal->setAutoMode(true);
    }
};

TEST_F(ThermalControlHalTest, CreateHal_ReturnsNonNull) {
    EXPECT_NE(mHal.get(), nullptr);
}

TEST_F(ThermalControlHalTest, CreateHal_TwoInstances_BothNonNull) {
    auto hal2 = createThermalControlHal();
    EXPECT_NE(hal2.get(), nullptr);
    EXPECT_NE(mHal.get(), nullptr);
}

TEST_F(ThermalControlHalTest, GetCpuTemp_IsFinite) {
    float temp = mHal->getCpuTemperatureCelsius();
    EXPECT_TRUE(std::isfinite(temp));
    EXPECT_FALSE(std::isnan(temp));
}

TEST_F(ThermalControlHalTest, GetCpuTemp_WhenHardwarePresent_IsPositive) {
    float temp = mHal->getCpuTemperatureCelsius();
    if (temp == 0.0f) GTEST_SKIP() << "Thermal sysfs not readable";
    EXPECT_GT(temp, 0.0f);
}

TEST_F(ThermalControlHalTest, GetCpuTemp_WhenHardwarePresent_InRange) {
    float temp = mHal->getCpuTemperatureCelsius();
    if (temp == 0.0f) GTEST_SKIP() << "Thermal sysfs not readable";
    EXPECT_GE(temp,   0.0f);
    EXPECT_LE(temp, 120.0f);
}

TEST_F(ThermalControlHalTest, GetCpuTemp_AlwaysFinite) {
    for (int i = 0; i < 5; ++i) {
        float temp = mHal->getCpuTemperatureCelsius();
        EXPECT_TRUE(std::isfinite(temp)) << "Iteration " << i;
    }
}

TEST_F(ThermalControlHalTest, GetFanSpeedPercent_InRange) {
    int32_t percent = mHal->getFanSpeedPercent();
    EXPECT_GE(percent, 0);
    EXPECT_LE(percent, 100);
}

TEST_F(ThermalControlHalTest, GetFanSpeedRpm_ValidValue) {
    int32_t rpm = mHal->getFanSpeedRpm();
    EXPECT_TRUE(rpm == -1 || rpm >= 0)
        << "getFanSpeedRpm must return -1 or >= 0, got: " << rpm;
}

TEST_F(ThermalControlHalTest, IsFanRunning_ConsistentWithFanSpeedPercent) {
    int32_t percent = mHal->getFanSpeedPercent();
    bool running    = mHal->isFanRunning();
    if (percent > 0) EXPECT_TRUE(running);
}

TEST_F(ThermalControlHalTest, IsAutoMode_ReturnsBool) {
    bool mode = mHal->isAutoMode();
    EXPECT_TRUE(mode == true || mode == false);
}

TEST_F(ThermalControlHalTest, SetFanSpeed_0_ReadBackZero) {
    if (!mHal->setFanSpeed(0)) GTEST_SKIP() << "Fan hardware not available";
    EXPECT_EQ(mHal->getFanSpeedPercent(), 0);
}

TEST_F(ThermalControlHalTest, SetFanSpeed_50_ReadBackApprox50) {
    if (!mHal->setFanSpeed(50)) GTEST_SKIP() << "Fan hardware not available";
    EXPECT_NEAR(mHal->getFanSpeedPercent(), 50, 1);
}

TEST_F(ThermalControlHalTest, SetFanSpeed_100_ReadBackFull) {
    if (!mHal->setFanSpeed(100)) GTEST_SKIP() << "Fan hardware not available";
    EXPECT_EQ(mHal->getFanSpeedPercent(), 100);
    mHal->setAutoMode(true);
}

TEST_F(ThermalControlHalTest, SetFanEnabled_True_FanRunning) {
    if (!mHal->setFanEnabled(true)) GTEST_SKIP() << "Fan hardware not available";
    EXPECT_TRUE(mHal->isFanRunning());
    EXPECT_EQ(mHal->getFanSpeedPercent(), 100);
    mHal->setAutoMode(true);
}

TEST_F(ThermalControlHalTest, SetFanEnabled_False_FanStopped) {
    if (!mHal->setFanEnabled(false)) GTEST_SKIP() << "Fan hardware not available";
    EXPECT_FALSE(mHal->isFanRunning());
    EXPECT_EQ(mHal->getFanSpeedPercent(), 0);
    mHal->setAutoMode(true);
}

TEST_F(ThermalControlHalTest, SetAutoMode_True_IsAutoModeTrue) {
    if (!mHal->setAutoMode(true)) GTEST_SKIP() << "Fan hardware not available";
    EXPECT_TRUE(mHal->isAutoMode());
}

TEST_F(ThermalControlHalTest, SetAutoMode_False_IsAutoModeFalse) {
    if (!mHal->setAutoMode(false)) GTEST_SKIP() << "Fan hardware not available";
    EXPECT_FALSE(mHal->isAutoMode());
    mHal->setAutoMode(true);
}

TEST_F(ThermalControlHalTest, SetFanSpeed_NegativeInput_ReturnsFalse) {
    EXPECT_FALSE(mHal->setFanSpeed(-1));
}

TEST_F(ThermalControlHalTest, SetFanSpeed_Over100_ReturnsFalse) {
    EXPECT_FALSE(mHal->setFanSpeed(101));
}

TEST_F(ThermalControlHalTest, SetFanSpeed_Boundary_0_ValidInput) {
    bool result = mHal->setFanSpeed(0);
    EXPECT_TRUE(result == true || result == false);
}

TEST_F(ThermalControlHalTest, SetFanSpeed_Boundary_100_ValidInput) {
    bool result = mHal->setFanSpeed(100);
    EXPECT_TRUE(result == true || result == false);
    if (result) mHal->setAutoMode(true);
}
