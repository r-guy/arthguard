package com.example.arthguard.features.settings.presentation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.arthguard.BuildConfig
import com.example.arthguard.core.util.sms.SmsClassifier
import com.example.arthguard.features.settings.presentation.viewmodel.SettingsViewModel
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Settings") }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SettingsItem(
                icon = Icons.Rounded.History,
                title = "SMS Import Days",
                subtitle = "Import transactions from last 30 days",
                onClick = { }
            )
            SettingsItem(
                icon = Icons.Rounded.Category,
                title = "Manage Categories",
                subtitle = "Add or edit expense categories",
                onClick = { }
            )
            SettingsItem(
                icon = Icons.Rounded.FileDownload,
                title = "Export",
                subtitle = "Export expenses to file",
                onClick = { viewModel.setShowExportSheet(true) }
            )
            SettingsItem(
                icon = Icons.Rounded.DeleteForever,
                title = "Clear All Data",
                subtitle = "Delete all expenses and budgets",
                onClick = { }
            )
            SettingsItem(
                icon = Icons.Rounded.Info,
                title = "About",
                subtitle = "Version 1.0.0",
                onClick = { }
            )
        }
    }

    if (uiState.showExportSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.setShowExportSheet(false) },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column {
                SettingsItem(
                    icon = Icons.Rounded.TableChart,
                    title = "Export as CSV",
                    subtitle = "Spreadsheet format",
                    onClick = { viewModel.exportToCsv(context, uiState.expenses) }
                )
                SettingsItem(
                    icon = Icons.Rounded.Code,
                    title = "Export as JSON",
                    subtitle = "Developer format",
                    onClick = { viewModel.exportToJson(context, uiState.expenses) }
                )
            }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    ListItem(
        modifier = Modifier.clickable(
            onClick = onClick
        ),
        leadingContent = { Icon(icon, contentDescription = null) },
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) }
    )
}
