package com.example.arthguard.features.settings.presentation.viewmodel

import com.example.arthguard.BuildConfig
import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.arthguard.core.util.sms.RegexParser
import com.example.arthguard.core.util.sms.SmsClassifier
import com.example.arthguard.features.dashboard.domain.model.ExpenseModel
import com.example.arthguard.features.dashboard.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class SettingsUiState(
    val expenses: List<ExpenseModel> = emptyList(),
    val showExportSheet: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val _showExportSheet = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = combine(
        repository.getAllExpenses(),
        _showExportSheet
    ) { expenses, showExport ->
        SettingsUiState(expenses = expenses, showExportSheet = showExport)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setShowExportSheet(show: Boolean) { _showExportSheet.value = show }

    fun exportToCsv(context: Context, expenses: List<ExpenseModel>) {
        _showExportSheet.value = false
//        Timber.d("Export started!")
        viewModelScope.launch(Dispatchers.IO) {
            try {
//                Timber.d("Export started1!")
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                val classifier = if (BuildConfig.DEBUG) SmsClassifier(context) else null
//                Timber.d("Export started!2")
                val csv = buildString {
                    if (BuildConfig.DEBUG) {
                        appendLine("ID,Amount,Date,Category,Receiver,Type,Source,TypeRegex,TypeML,RawMessage")
                    } else {
                        appendLine("ID,Amount,Date,Category,Receiver,Type,Source,RawMessage")
                    }
                    expenses.forEach { e ->
                        val date = e.time?.let { dateFormat.format(Date(it)) } ?: ""
                        val category = e.category?.let { it::class.simpleName } ?: ""
                        if (BuildConfig.DEBUG) {
                            val typeRegex = e.rawMessage?.let { RegexParser.classifyType(it) } ?: ""
                            val typeML = e.rawMessage?.let { classifier?.classify(it) } ?: ""
                            appendLine("${e.id},${e.amount},$date,$category,\"${e.receiver ?: ""}\",${e.type},${e.source},$typeRegex,$typeML,\"${e.rawMessage ?: ""}\"")
                        } else {
                            appendLine("${e.id},${e.amount},$date,$category,\"${e.receiver ?: ""}\",${e.type},${e.source},\"${e.rawMessage ?: ""}\"")
                        }
                    }
                }
//                Timber.d("Export started!3")
//                Timber.d("Export success!")
                Handler(Looper.getMainLooper()).post {
//                    Timber.d("Export success!2")
                    Toast.makeText(context, "Export success!", Toast.LENGTH_LONG).show()
                }
                saveToDownloads(context, "arthguard_expenses.csv", csv, "text/csv")
            } catch (e: Exception) {
                Timber.d("Export failed: ${e.message}")
                Handler(Looper.getMainLooper()).post {
//                    Timber.d("Export failed2: ${e.message}")
                    Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun exportToJson(context: Context, expenses: List<ExpenseModel>) {
        _showExportSheet.value = false
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val classifier = if (BuildConfig.DEBUG) SmsClassifier(context) else null
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
                        if (BuildConfig.DEBUG) {
                            put("typeRegex", e.rawMessage?.let { RegexParser.classifyType(it) })
                            put("typeML", e.rawMessage?.let { classifier?.classify(it) })
                        }
                        put("rawMessage", e.rawMessage)
                    })
                }
                saveToDownloads(context, "arthguard_expenses.json", jsonArray.toString(2), "application/json")
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun saveToDownloads(context: Context, filename: String, content: String, mimeType: String) {
        try {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, filename)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            uri?.let { context.contentResolver.openOutputStream(it)?.use { os -> os.write(content.toByteArray()) } }
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Exported to Downloads/$filename", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
