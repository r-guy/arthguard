package com.example.arthguard.features.dashboard.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE duration = :duration")
    fun getBudget(duration: String): Flow<BudgetEntity?>

    @Query("SELECT * FROM budgets")
    fun getAllBudgets(): Flow<List<BudgetEntity>>

    @Upsert
    suspend fun saveBudget(budget: BudgetEntity)
}
