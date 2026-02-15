package com.example.arthguard.features.dashboard.domain.model

import java.util.Calendar

enum class DurationFilter(val label: String) {
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    THIS_QUARTER("This Quarter"),
    THIS_YEAR("This Year");

    fun getStartTime(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        when (this) {
            TODAY -> {}
            THIS_WEEK -> cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
            THIS_MONTH -> cal.set(Calendar.DAY_OF_MONTH, 1)
            THIS_QUARTER -> {
                val month = cal.get(Calendar.MONTH)
                cal.set(Calendar.MONTH, month - month % 3)
                cal.set(Calendar.DAY_OF_MONTH, 1)
            }
            THIS_YEAR -> cal.set(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }
}
