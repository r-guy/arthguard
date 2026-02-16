package com.example.arthguard.features.dashboard.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val duration: String,
    val amount: Double,
    val lastBudget: Double?,
    val updatedAt: Long
)
