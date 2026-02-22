package com.example.arthguard.core.util.sms

import android.content.Context
import android.provider.Telephony
import com.example.arthguard.features.dashboard.domain.model.ExpenseModel

object SmsReader {

    fun readPastTransactions(context: Context, daysBack: Int = 30): List<ExpenseModel> {
        val expenses = mutableListOf<ExpenseModel>()
        val cutoffTime = System.currentTimeMillis() - (daysBack * 24 * 60 * 60 * 1000L)

        val cursor = context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
            "${Telephony.Sms.DATE} > ?",
            arrayOf(cutoffTime.toString()),
            "${Telephony.Sms.DATE} DESC"
        )

        cursor?.use {
            val addressIdx = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIdx = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIdx = it.getColumnIndexOrThrow(Telephony.Sms.DATE)

            while (it.moveToNext()) {
                val sender = it.getString(addressIdx) ?: continue
                val body = it.getString(bodyIdx) ?: continue
                val date = it.getLong(dateIdx)

                SmsExpenseParser.parse(sender, body)?.copy(time = date)?.let { expense ->
                    expenses.add(expense)
                }
            }
        }
        return expenses
    }
}
