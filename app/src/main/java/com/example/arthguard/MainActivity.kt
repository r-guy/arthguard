package com.example.arthguard

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.arthguard.core.data.local.AppDatabase
import com.example.arthguard.core.navigation.AppNavigation
import com.example.arthguard.core.util.sms.SmsReader
import com.example.arthguard.features.dashboard.data.local.ExpenseEntity
import com.example.arthguard.ui.theme.ArthGuardTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val smsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.READ_SMS] == true) {
            importPastSms()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestSmsPermissionIfNeeded()
        setContent {
            ArthGuardTheme {
                AppNavigation()
            }
        }
    }

    private fun requestSmsPermissionIfNeeded() {
        val permissions = arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
        val needsPermission = permissions.any {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needsPermission) {
            smsPermissionLauncher.launch(permissions)
        } else {
            importPastSms()
        }
    }

    private fun importPastSms() {
        val dao = AppDatabase.getInstance(this).expenseDao()
        CoroutineScope(Dispatchers.IO).launch {
            val expenses = SmsReader.readPastTransactions(this@MainActivity, daysBack = 360)
            expenses.forEach { expense ->
                if (expense.rawMessage != null && dao.existsByRawMessage(expense.rawMessage)) return@forEach
                dao.insert(
                    ExpenseEntity(
                        amount = expense.amount,
                        time = expense.time,
                        category = null,
                        receiver = expense.receiver,
                        type = expense.type,
                        source = expense.source,
                        rawMessage = expense.rawMessage,
                        sender = expense.sender
                    )
                )
            }
        }
    }
}
