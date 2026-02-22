package com.example.arthguard.core.util.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.arthguard.core.data.local.AppDatabase
import com.example.arthguard.features.dashboard.data.local.ExpenseEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val fullMessage = messages.joinToString("") { it.messageBody }
        val sender = messages.firstOrNull()?.originatingAddress ?: return

        val expense = SmsExpenseParser.parse(sender, fullMessage) ?: return

        val dao = AppDatabase.getInstance(context).expenseDao()
        CoroutineScope(Dispatchers.IO).launch {
            if (dao.existsByRawMessage(fullMessage)) return@launch
            dao.insert(
                ExpenseEntity(
                    amount = expense.amount,
                    time = expense.time,
                    category = expense.category?.toString(),
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
