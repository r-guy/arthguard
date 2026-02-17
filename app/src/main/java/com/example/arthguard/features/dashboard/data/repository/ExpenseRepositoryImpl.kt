package com.example.arthguard.features.dashboard.data.repository

import com.example.arthguard.core.util.Result
import com.example.arthguard.features.dashboard.data.local.ExpenseDao
import com.example.arthguard.features.dashboard.data.local.ExpenseEntity
import com.example.arthguard.features.dashboard.domain.model.ExpenseCategory
import com.example.arthguard.features.dashboard.domain.model.ExpenseModel
import com.example.arthguard.features.dashboard.domain.model.TransactionSource
import com.example.arthguard.features.dashboard.domain.model.TransactionType
import com.example.arthguard.features.dashboard.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ExpenseRepositoryImpl(private val dao: ExpenseDao) : ExpenseRepository {

    override suspend fun saveExpense(expense: ExpenseModel): Result<Unit> {
        return try {
            dao.insert(expense.toEntity())
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override fun getAllExpenses(): Flow<List<ExpenseModel>> {
        return dao.getAll().map { list -> list.map { it.toModel() } }
    }

    private fun ExpenseModel.toEntity() = ExpenseEntity(
        amount = amount,
        time = time,
        category = category?.let { it::class.simpleName },
        receiver = receiver,
        type = type,
        source = source,
        rawMessage = rawMessage,
        sender = sender
    )

    private fun ExpenseEntity.toModel() = ExpenseModel(
        id = id.toString(),
        amount = amount,
        time = time,
        category = category?.toExpenseCategory(),
        receiver = receiver,
        type = type,
        source = source,
        rawMessage = rawMessage,
        sender = sender
    )

    private fun String.toExpenseCategory(): ExpenseCategory = when (this) {
        "Food" -> ExpenseCategory.Food
        "Travelling" -> ExpenseCategory.Travelling
        "Groceries" -> ExpenseCategory.Groceries
        "Entertainment" -> ExpenseCategory.Entertainment
        "Shopping" -> ExpenseCategory.Shopping
        "Bills" -> ExpenseCategory.Bills
        else -> ExpenseCategory.Other
    }
}
