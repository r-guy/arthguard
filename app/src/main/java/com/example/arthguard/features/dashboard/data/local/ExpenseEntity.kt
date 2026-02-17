package com.example.arthguard.features.dashboard.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double?,
    val time: Long?,
    val category: String?,
    val receiver: String?,
    val type: String = "DEBIT",
    val source: String = "MANUAL",
    val rawMessage: String? = null,
    val sender: String? = null
)
