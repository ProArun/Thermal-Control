// Copyright (C) 2024 MyOEM
// SPDX-License-Identifier: Apache-2.0

package com.myoem.thermalmonitor

import android.os.Bundle
import android.os.ServiceManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import com.myoem.thermalcontrol.ThermalControlManager
import com.myoem.thermalmonitor.ui.ThermalScreen
import com.myoem.thermalmonitor.ui.theme.ThermalMonitorTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binder  = ServiceManager.checkService(ThermalControlManager.SERVICE_NAME)
        val manager = ThermalControlManager(binder)

        val viewModel = ViewModelProvider(
            this,
            ThermalViewModelFactory(manager)
        )[ThermalViewModel::class.java]

        setContent {
            ThermalMonitorTheme {
                ThermalScreen(viewModel = viewModel)
            }
        }
    }
}
