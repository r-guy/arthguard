package com.example.arthguard.features.dashboard.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.arthguard.features.dashboard.data.local.BudgetDao
import com.example.arthguard.features.dashboard.data.local.BudgetEntity
import com.example.arthguard.features.dashboard.domain.model.DurationFilter
import com.example.arthguard.features.dashboard.domain.model.ExpenseModel
import com.example.arthguard.features.dashboard.domain.model.TrendDuration
import com.example.arthguard.features.dashboard.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val expenses: List<ExpenseModel> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val budgetDuration: DurationFilter = DurationFilter.THIS_MONTH,
    val selectedDuration: DurationFilter = DurationFilter.THIS_MONTH,
    val trendDuration: TrendDuration = TrendDuration.WEEK,
    val receiversDuration: DurationFilter = DurationFilter.THIS_MONTH,
    val categoriesDuration: DurationFilter = DurationFilter.THIS_MONTH,
    val showAddExpenseSheet: Boolean = false,
    val showBudgetSheet: Boolean = false
) {
    fun getBudgetForDuration(duration: DurationFilter): BudgetEntity? =
        budgets.find { it.duration == duration.name }
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: ExpenseRepository,
    private val budgetDao: BudgetDao
) : ViewModel() {

    private val _budgetDuration = MutableStateFlow(DurationFilter.THIS_MONTH)
    private val _selectedDuration = MutableStateFlow(DurationFilter.THIS_MONTH)
    private val _trendDuration = MutableStateFlow(TrendDuration.WEEK)
    private val _receiversDuration = MutableStateFlow(DurationFilter.THIS_MONTH)
    private val _categoriesDuration = MutableStateFlow(DurationFilter.THIS_MONTH)
    private val _showAddExpenseSheet = MutableStateFlow(false)
    private val _showBudgetSheet = MutableStateFlow(false)

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.getAllExpenses(),
        budgetDao.getAllBudgets(),
        _budgetDuration,
        _selectedDuration,
        _trendDuration,
        _receiversDuration,
        _categoriesDuration,
        combine(_showAddExpenseSheet, _showBudgetSheet) { a, b -> a to b }
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val sheets = values[7] as Pair<Boolean, Boolean>
        DashboardUiState(
            expenses = values[0] as List<ExpenseModel>,
            budgets = values[1] as List<BudgetEntity>,
            budgetDuration = values[2] as DurationFilter,
            selectedDuration = values[3] as DurationFilter,
            trendDuration = values[4] as TrendDuration,
            receiversDuration = values[5] as DurationFilter,
            categoriesDuration = values[6] as DurationFilter,
            showAddExpenseSheet = sheets.first,
            showBudgetSheet = sheets.second
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    fun setBudgetDuration(duration: DurationFilter) { _budgetDuration.value = duration }
    fun setSelectedDuration(duration: DurationFilter) { _selectedDuration.value = duration }
    fun setTrendDuration(duration: TrendDuration) { _trendDuration.value = duration }
    fun setReceiversDuration(duration: DurationFilter) { _receiversDuration.value = duration }
    fun setCategoriesDuration(duration: DurationFilter) { _categoriesDuration.value = duration }
    fun setShowAddExpenseSheet(show: Boolean) { _showAddExpenseSheet.value = show }
    fun setShowBudgetSheet(show: Boolean) { _showBudgetSheet.value = show }

    fun saveExpense(expense: ExpenseModel) {
        viewModelScope.launch {
            repository.saveExpense(expense)
            _showAddExpenseSheet.value = false
        }
    }

    fun saveBudget(duration: DurationFilter, amount: Double) {
        viewModelScope.launch {
            budgetDao.saveBudget(
                BudgetEntity(
                    duration = duration.name,
                    amount = amount,
                    lastBudget = null,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }
}
