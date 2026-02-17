package com.example.arthguard.features.dashboard.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Insert
    suspend fun insert(expense: ExpenseEntity)

    @Query("SELECT * FROM expenses ORDER BY time DESC")
    fun getAll(): Flow<List<ExpenseEntity>>

    @Query("UPDATE expenses SET category = :category WHERE id = :id")
    suspend fun updateCategory(id: Long, category: String)

    @Query("UPDATE expenses SET amount = :amount, receiver = :receiver, category = :category WHERE id = :id")
    suspend fun update(id: Long, amount: Double?, receiver: String?, category: String?)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT EXISTS(SELECT 1 FROM expenses WHERE rawMessage = :rawMessage)")
    suspend fun existsByRawMessage(rawMessage: String): Boolean
}
