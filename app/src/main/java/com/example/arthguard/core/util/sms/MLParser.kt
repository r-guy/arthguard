package com.example.arthguard.core.util.sms

import android.content.Context
import com.example.arthguard.core.util.sms.RegexParser.isTransactionSms
import com.example.arthguard.features.dashboard.domain.model.ExpenseModel
import com.example.arthguard.features.dashboard.domain.model.TransactionSource
import com.example.arthguard.features.dashboard.domain.model.TransactionType

class MLParser(context: Context) {

    private val classifier = SmsClassifier(context)

    fun parse(sender: String, message: String): ExpenseModel? {
        if (!isTransactionSms(message)) return null
        val classification = classifier.classify(message)
        if (classification == "NON_TRANSACTION") return null

        val amount = RegexParser.extractAmount(message) ?: return null

        return ExpenseModel(
            amount = amount,
            type = if (classification == "DEBIT") TransactionType.DEBIT else TransactionType.CREDIT,
            receiver = RegexParser.extractMerchant(message),
            time = System.currentTimeMillis(),
            source = TransactionSource.SMS,
            rawMessage = message,
            sender = sender
        )
    }
}
