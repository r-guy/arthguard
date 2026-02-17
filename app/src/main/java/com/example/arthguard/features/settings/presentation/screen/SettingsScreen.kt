package com.example.arthguard.features.settings.presentation.screen

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.widget.Toast
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.example.arthguard.core.data.local.AppDatabase
import com.example.arthguard.features.dashboard.data.repository.ExpenseRepositoryImpl
import com.example.arthguard.features.dashboard.domain.model.ExpenseModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember { AppDatabase.getInstance(context).expenseDao() }
    val repository = remember { ExpenseRepositoryImpl(dao) }
    val expenses by repository.getAllExpenses().collectAsState(initial = emptyList())
    var showExportSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text("Settings") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SettingsItem(
                icon = Icons.Default.History,
                title = "SMS Import Days",
                subtitle = "Import transactions from last 30 days",
                onClick = { }
            )
            SettingsItem(
                icon = Icons.Default.Category,
                title = "Manage Categories",
                subtitle = "Add or edit expense categories",
                onClick = { }
            )
            SettingsItem(
                icon = Icons.Default.FileDownload,
                title = "Export",
                subtitle = "Export expenses to file",
                onClick = { showExportSheet = true }
            )
            SettingsItem(
                icon = Icons.Default.DeleteForever,
                title = "Clear All Data",
                subtitle = "Delete all expenses and budgets",
                onClick = { }
            )
            SettingsItem(
                icon = Icons.Default.Info,
                title = "About",
                subtitle = "Version 1.0.0",
                onClick = { }
            )
        }
    }

    if (showExportSheet) {
        ModalBottomSheet(
            onDismissRequest = { showExportSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column {
                SettingsItem(
                    icon = Icons.Default.TableChart,
                    title = "Export as CSV",
                    subtitle = "Spreadsheet format",
                    onClick = {
                        showExportSheet = false
                        scope.launch(Dispatchers.IO) { exportToCsv(context, expenses) }
                    }
                )
                SettingsItem(
                    icon = Icons.Default.Code,
                    title = "Export as JSON",
                    subtitle = "Developer format",
                    onClick = {
                        showExportSheet = false
                        scope.launch(Dispatchers.IO) { exportToJson(context, expenses) }
                    }
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
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { Icon(icon, contentDescription = null) },
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) }
    )
}

private fun exportToCsv(context: Context, expenses: List<ExpenseModel>) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val csv = buildString {
        appendLine("ID,Amount,Date,Category,Receiver,Type,Source")
        expenses.forEach { e ->
            val date = e.time?.let { dateFormat.format(Date(it)) } ?: ""
            val category = e.category?.let { it::class.simpleName } ?: ""
            appendLine("${e.id},${e.amount},$date,$category,\"${e.receiver ?: ""}\",${e.type},${e.source}")
        }
    }
    saveToDownloads(context, "arthguard_expenses.csv", csv, "text/csv")
}

private fun exportToJson(context: Context, expenses: List<ExpenseModel>) {
    val jsonArray = JSONArray()
    expenses.forEach { e ->
        jsonArray.put(JSONObject().apply {
            put("id", e.id)
            put("amount", e.amount)
            put("time", e.time)
            put("category", e.category?.let { it::class.simpleName })
            put("receiver", e.receiver)
            put("type", e.type)
            put("source", e.source)
        })
    }
    saveToDownloads(context, "arthguard_expenses.json", jsonArray.toString(2), "application/json")
}

private fun saveToDownloads(context: Context, filename: String, content: String, mimeType: String) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, filename)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { os ->
                    os.write(content.toByteArray())
                }
            }
        } else {
            val file = java.io.File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                filename
            )
            file.writeText(content)
        }
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, "Exported to Downloads/$filename", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
