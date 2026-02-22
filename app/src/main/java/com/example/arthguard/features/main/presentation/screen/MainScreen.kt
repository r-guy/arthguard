package com.example.arthguard.features.main.presentation.screen

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.arthguard.features.dashboard.presentation.screen.DashboardScreen
import com.example.arthguard.features.main.presentation.viewmodel.MainViewModel
import com.example.arthguard.features.settings.presentation.screen.SettingsScreen

@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onNavigateToExpenseBreakup: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.setSelectedTab(0) },
                    icon = { Icon(Icons.Rounded.Home, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") }
                )
                NavigationBarItem(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.setSelectedTab(1) },
                    icon = { Icon(Icons.Rounded.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") }
                )
            }
        }
    ) { padding ->
        when (uiState.selectedTab) {
            0 -> DashboardScreen(
                modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
                onNavigateToExpenseBreakup = onNavigateToExpenseBreakup
            )
            1 -> SettingsScreen(modifier = Modifier.padding(padding))
        }
    }
}
