package com.example.arthguard.core.util.sms

import com.example.arthguard.features.dashboard.domain.model.ExpenseModel
import com.example.arthguard.features.dashboard.domain.model.TransactionSource
import com.example.arthguard.features.dashboard.domain.model.TransactionType

object SmsExpenseParser {

    private val debitPatterns = listOf(
        Regex("""(?i)debited\s*(?:by\s*)?(?:rs\.?|inr\.?|₹)?\s*([\d,]+\.?\d*)"""),
        Regex("""(?i)(?:rs\.?|inr\.?|₹)\s*([\d,]+\.?\d*)\s*(?:debited|withdrawn)"""),
        Regex("""(?i)spent\s*(?:rs\.?|inr\.?|₹)\s*([\d,]+\.?\d*)"""),
        Regex("""(?i)(?<!\bnon\s)(?<!\bno\s)(?<!\bfailed\s)(?<!\bpending\s)(?<!\bunsuccessful\s)(?<!\bdeclined\s)(?<!\brejected\s)payment\s*(?:of\s*)?(?:rs\.?|inr\.?|₹)\s*([\d,]+(?:\.\d{1,2})?)"""),
        Regex("""(?i)purchased\s*(?:for\s*)?(?:rs\.?|inr\.?|₹)\s*([\d,]+\.?\d*)"""),
        Regex("""(?i)upi-mandate\s*(?:for\s*)?(?:rs\.?|inr\.?|₹)?\s*([\d,]+\.?\d*)"""),
        Regex("""(?i)(?:rs\.?|inr\.?|₹)\s*([\d,]+\.?\d*)\s*(?:paid|sent|transferred)""")
    )

    private val creditPatterns = listOf(
        Regex("""(?i)credited\s*(?:by\s*)?(?:rs\.?|inr\.?|₹)?\s*([\d,]+\.?\d*)"""),
        Regex("""(?i)(?:rs\.?|inr\.?|₹)\s*([\d,]+\.?\d*)\s*credited"""),
        Regex("""(?i)received\s*(?:rs\.?|inr\.?|₹)\s*([\d,]+\.?\d*)"""),
        Regex("""(?i)refund\s*(?:of\s*)?(?:rs\.?|inr\.?|₹)\s*([\d,]+\.?\d*)""")
    )

    private val merchantPatterns = listOf(
        Regex("""(?i)towards\s+UPI/\w+/\w+/([^/]+)/"""),
        Regex("""(?i)towards\s+([A-Za-z][A-Za-z0-9\s]*?)(?:\s+from|\s+on|\s+ref|\.|\s*$)"""),
        Regex("""(?i)(?:trf|transfer)\s+to\s+(.+?)\s+ref"""),
        Regex("""(?i)(?:paid|sent)\s+to\s+([A-Za-z][A-Za-z0-9\s]*?)(?:\s+on|\s+ref|\.|\s*$)"""),
        Regex("""(?i)at\s+([A-Za-z][A-Za-z0-9\s]{2,}?)(?:\s+on|\s+ref|\.|\s*$)"""),
        Regex("""(?i)from\s+([A-Za-z][A-Za-z0-9\s]*?)\s+(?:credited|received)"""),
        Regex("""(?i)to\s+vpa\s+([A-Za-z0-9@._-]+)""")
    )

    fun parse(sender: String, message: String): ExpenseModel? {
        if (!isTransactionSms(message)) return null
        val (amount, type) = extractAmountAndType(message) ?: return null
        return ExpenseModel(
            amount = amount,
            type = type,
            receiver = extractMerchant(message),
            time = System.currentTimeMillis(),
            source = TransactionSource.SMS,
            rawMessage = message,
            sender = sender
        )
    }

    private fun isTransactionSms(message: String): Boolean {
        val keywords = listOf("debited", "credited", "spent", "received", "payment", "withdrawn", "transferred", "upi", "a/c", "acct", "account")
        return keywords.any { message.contains(it, ignoreCase = true) }
    }

    private fun extractAmountAndType(message: String): Pair<Double, String>? {
        for (pattern in debitPatterns) {
            pattern.find(message)?.groupValues?.get(1)?.let { amountStr ->
                amountStr.replace(",", "").toDoubleOrNull()?.let {
                    if (it > 0) return it to TransactionType.DEBIT
                }
            }
        }
        for (pattern in creditPatterns) {
            pattern.find(message)?.groupValues?.get(1)?.let { amountStr ->
                amountStr.replace(",", "").toDoubleOrNull()?.let {
                    if (it > 0) return it to TransactionType.CREDIT
                }
            }
        }
        return null
    }

    private fun extractMerchant(message: String): String? {
        for (pattern in merchantPatterns) {
            pattern.find(message)?.groupValues?.get(1)?.trim()?.let {
                if (it.isNotBlank() && it.length > 1) return it
            }
        }
        return null
    }
}
