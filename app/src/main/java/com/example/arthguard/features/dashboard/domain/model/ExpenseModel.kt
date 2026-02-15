package com.example.arthguard.features.dashboard.domain.model

data class ExpenseModel(
    val id: String? = null,
    val amount: Double? = null,
    val time: Long? = null,
    val category: ExpenseCategory? = null,
    val receiver: String? = null,
    val props: Map<String, Any>? = null
)

sealed class ExpenseCategory {
    data object Food : ExpenseCategory()
    data object Travelling : ExpenseCategory()
    data object Groceries : ExpenseCategory()
    data object Entertainment : ExpenseCategory()
    data object Shopping : ExpenseCategory()
    data object Bills : ExpenseCategory()
    data object Other : ExpenseCategory()
}
