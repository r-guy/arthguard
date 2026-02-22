package com.example.arthguard.features.expense_breakup.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.arthguard.core.util.Constants.amountStops
import com.example.arthguard.core.util.sms.SmsReader
import com.example.arthguard.features.dashboard.data.local.ExpenseDao
import com.example.arthguard.features.dashboard.data.local.ExpenseEntity
import com.example.arthguard.features.dashboard.domain.model.ExpenseCategory
import com.example.arthguard.features.dashboard.domain.model.ExpenseModel
import com.example.arthguard.features.dashboard.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExpenseBreakupUiState(
    val expenses: List<ExpenseModel> = emptyList(),
    val selectedCategory: ExpenseCategory? = null,
    val selectedReceiver: String? = null,
    val fromDate: Long? = null,
    val toDate: Long? = null,
    val amountRange: ClosedFloatingPointRange<Float> = 0f..11f,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val showFilterSheet: Boolean = false,
    val expenseToEdit: ExpenseModel? = null,
    val expenseToDelete: ExpenseModel? = null
) {
    val receivers: List<String> get() = expenses.mapNotNull { it.receiver }.distinct()

    val filteredExpenses: List<ExpenseModel> get() = expenses.filter { expense ->
        val categoryMatch = selectedCategory == null || expense.category == selectedCategory
        val receiverMatch = selectedReceiver == null || expense.receiver == selectedReceiver
        val fromMatch = fromDate == null || (expense.time ?: 0) >= fromDate
        val toMatch = toDate == null || (expense.time ?: 0) <= toDate
        val minAmount = amountStops[amountRange.start.toInt()]
        val maxAmount = amountStops[amountRange.endInclusive.toInt()]
        val amountMatch = (expense.amount ?: 0.0) >= minAmount && (expense.amount ?: 0.0) <= maxAmount
        val searchMatch = searchQuery.isBlank() ||
            expense.receiver?.contains(searchQuery, ignoreCase = true) == true ||
            expense.category?.let { it::class.simpleName?.contains(searchQuery, ignoreCase = true) } == true ||
            expense.amount?.toString()?.contains(searchQuery) == true
        categoryMatch && receiverMatch && fromMatch && toMatch && amountMatch && searchMatch
    }

    val groupedExpenses: Map<ExpenseCategory, List<ExpenseModel>> get() =
        filteredExpenses.groupBy { it.category ?: ExpenseCategory.Other }
}

@HiltViewModel
class ExpenseBreakupViewModel @Inject constructor(
    private val repository: ExpenseRepository,
    private val dao: ExpenseDao
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow<ExpenseCategory?>(null)
    private val _selectedReceiver = MutableStateFlow<String?>(null)
    private val _fromDate = MutableStateFlow<Long?>(null)
    private val _toDate = MutableStateFlow<Long?>(null)
    private val _amountRange = MutableStateFlow(0f..11f)
    private val _searchQuery = MutableStateFlow("")
    private val _isSearchActive = MutableStateFlow(false)
    private val _showFilterSheet = MutableStateFlow(false)
    private val _expenseToEdit = MutableStateFlow<ExpenseModel?>(null)
    private val _expenseToDelete = MutableStateFlow<ExpenseModel?>(null)

    val uiState: StateFlow<ExpenseBreakupUiState> = combine(
        repository.getAllExpenses(),
        _selectedCategory,
        _selectedReceiver,
        _fromDate,
        _toDate,
        _amountRange,
        _searchQuery,
        _isSearchActive,
        _showFilterSheet,
        combine(_expenseToEdit, _expenseToDelete) { edit, delete -> edit to delete }
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val editDelete = values[9] as Pair<ExpenseModel?, ExpenseModel?>
        ExpenseBreakupUiState(
            expenses = values[0] as List<ExpenseModel>,
            selectedCategory = values[1] as ExpenseCategory?,
            selectedReceiver = values[2] as String?,
            fromDate = values[3] as Long?,
            toDate = values[4] as Long?,
            amountRange = values[5] as ClosedFloatingPointRange<Float>,
            searchQuery = values[6] as String,
            isSearchActive = values[7] as Boolean,
            showFilterSheet = values[8] as Boolean,
            expenseToEdit = editDelete.first,
            expenseToDelete = editDelete.second
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ExpenseBreakupUiState())

    fun setSelectedCategory(category: ExpenseCategory?) { _selectedCategory.value = category }
    fun setSelectedReceiver(receiver: String?) { _selectedReceiver.value = receiver }
    fun setDateRange(from: Long?, to: Long?) { _fromDate.value = from; _toDate.value = to }
    fun setAmountRange(range: ClosedFloatingPointRange<Float>) { _amountRange.value = range }
    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setSearchActive(active: Boolean) { _isSearchActive.value = active; if (!active) _searchQuery.value = "" }
    fun setShowFilterSheet(show: Boolean) { _showFilterSheet.value = show }
    fun setExpenseToEdit(expense: ExpenseModel?) { _expenseToEdit.value = expense }
    fun setExpenseToDelete(expense: ExpenseModel?) { _expenseToDelete.value = expense }

    fun updateExpense(expense: ExpenseModel) {
        expense.id?.toLongOrNull()?.let { id ->
            viewModelScope.launch {
                dao.update(id, expense.amount, expense.receiver, expense.category?.let { it::class.simpleName })
            }
        }
        _expenseToEdit.value = null
    }

    fun deleteExpense(expense: ExpenseModel) {
        expense.id?.toLongOrNull()?.let { id ->
            viewModelScope.launch { dao.delete(id) }
        }
        _expenseToDelete.value = null
    }

    fun refreshFromSms(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val expenses = SmsReader.readPastTransactions(context, daysBack = 360)
            expenses.forEach { expense ->
                if (expense.rawMessage != null && dao.existsByRawMessage(expense.rawMessage)) return@forEach
                dao.insert(
                    ExpenseEntity(
                        amount = expense.amount,
                        time = expense.time,
                        category = null,
                        receiver = expense.receiver,
                        type = expense.type,
                        source = expense.source,
                        rawMessage = expense.rawMessage,
                        sender = expense.sender
                    )
                )
            }
        }
    }
}
