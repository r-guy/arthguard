package com.example.arthguard.features.dashboard.domain.model

import java.util.Calendar

enum class TrendDuration(val label: String) {
    WEEK("Week"),
    MONTH("Month"),
    QUARTER("Quarter"),
    YEAR("Year");

    fun getDataPoints(expenses: List<ExpenseModel>): List<Pair<String, Double>> {
        val cal = Calendar.getInstance()
        return when (this) {
            WEEK -> (6 downTo 0).map { daysAgo ->
                cal.timeInMillis = System.currentTimeMillis()
                cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
                val dayStart = cal.apply {
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val dayEnd = dayStart + 86400000L
                val label = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")[cal.get(Calendar.DAY_OF_WEEK) - 1]
                label to expenses.filter { (it.time ?: 0) in dayStart until dayEnd }.sumOf { it.amount ?: 0.0 }
            }
            MONTH -> (3 downTo 0).map { weeksAgo ->
                cal.timeInMillis = System.currentTimeMillis()
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.add(Calendar.WEEK_OF_YEAR, -weeksAgo)
                val weekStart = cal.apply {
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val weekEnd = weekStart + 7 * 86400000L
                "W${4 - weeksAgo}" to expenses.filter { (it.time ?: 0) in weekStart until weekEnd }.sumOf { it.amount ?: 0.0 }
            }
            QUARTER -> (2 downTo 0).map { monthsAgo ->
                cal.timeInMillis = System.currentTimeMillis()
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.add(Calendar.MONTH, -monthsAgo)
                val monthStart = cal.apply {
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                cal.add(Calendar.MONTH, 1)
                val monthEnd = cal.timeInMillis
                val label = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")[cal.get(Calendar.MONTH).let { if (it == 0) 11 else it - 1 }]
                label to expenses.filter { (it.time ?: 0) in monthStart until monthEnd }.sumOf { it.amount ?: 0.0 }
            }
            YEAR -> (3 downTo 0).map { quartersAgo ->
                cal.timeInMillis = System.currentTimeMillis()
                val currentMonth = cal.get(Calendar.MONTH)
                cal.set(Calendar.MONTH, currentMonth - currentMonth % 3)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.add(Calendar.MONTH, -quartersAgo * 3)
                val quarterStart = cal.apply {
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                cal.add(Calendar.MONTH, 3)
                val quarterEnd = cal.timeInMillis
                "Q${4 - quartersAgo}" to expenses.filter { (it.time ?: 0) in quarterStart until quarterEnd }.sumOf { it.amount ?: 0.0 }
            }
        }
    }
}
