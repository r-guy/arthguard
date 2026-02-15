package com.example.arthguard.features.dashboard.domain.repository

import com.example.arthguard.core.util.Result
import com.example.arthguard.features.dashboard.domain.model.ExpenseModel
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    suspend fun saveExpense(expense: ExpenseModel): Result<Unit>
    fun getAllExpenses(): Flow<List<ExpenseModel>>
}
