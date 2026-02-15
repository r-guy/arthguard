package com.example.arthguard.features.expense_breakup.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.arthguard.core.data.local.AppDatabase
import com.example.arthguard.core.util.AppColors
import com.example.arthguard.core.util.Constants.amountStops
import com.example.arthguard.features.dashboard.data.repository.ExpenseRepositoryImpl
import com.example.arthguard.features.dashboard.domain.model.ExpenseCategory
import com.example.arthguard.features.dashboard.domain.model.ExpenseModel
import com.example.arthguard.features.expense_breakup.presentation.components.ExpenseFilterBottomSheet
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseBreakupScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember { ExpenseRepositoryImpl(AppDatabase.getInstance(context).expenseDao()) }
    val expenses by repository.getAllExpenses().collectAsState(initial = emptyList())

    var selectedCategory by remember { mutableStateOf<ExpenseCategory?>(null) }
    var selectedReceiver by remember { mutableStateOf<String?>(null) }
    var fromDate by remember { mutableStateOf<Long?>(null) }
    var toDate by remember { mutableStateOf<Long?>(null) }
    var amountRange by remember { mutableStateOf(0f..11f) }
    var showFilterSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val receivers by remember(expenses) {
        derivedStateOf { expenses.mapNotNull { it.receiver }.distinct() }
    }

    val filteredExpenses by remember(expenses, selectedCategory, selectedReceiver, fromDate, toDate, amountRange) {
        derivedStateOf {
            expenses.filter { expense ->
                val categoryMatch = selectedCategory == null || expense.category == selectedCategory
                val receiverMatch = selectedReceiver == null || expense.receiver == selectedReceiver
                val fromMatch = fromDate == null || (expense.time ?: 0) >= fromDate!!
                val toMatch = toDate == null || (expense.time ?: 0) <= toDate!!
                val minAmount = amountStops[amountRange.start.toInt()]
                val maxAmount = amountStops[amountRange.endInclusive.toInt()]
                val amountMatch = (expense.amount ?: 0.0) >= minAmount && (expense.amount ?: 0.0) <= maxAmount
                categoryMatch && receiverMatch && fromMatch && toMatch && amountMatch
            }
        }
    }

    val groupedExpenses by remember(filteredExpenses) {
        derivedStateOf {
            filteredExpenses.groupBy { it.category ?: ExpenseCategory.Other }
        }
    }

    val expandedCategories = remember { mutableStateMapOf<ExpenseCategory, Boolean>() }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Expense Breakup") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            groupedExpenses.forEach { (category, categoryExpenses) ->
                val isExpanded = expandedCategories[category] ?: true
                val categoryTotal = categoryExpenses.sumOf { it.amount ?: 0.0 }

                item(key = "header_$category") {
                    CategoryHeader(
                        category = category,
                        count = categoryExpenses.size,
                        total = categoryTotal,
                        expanded = isExpanded,
                        onToggle = { expandedCategories[category] = !isExpanded }
                    )
                }

                if (isExpanded) {
                    items(categoryExpenses, key = { it.id ?: it.hashCode() }) { expense ->
                        ExpenseItem(expense)
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        ExpenseFilterBottomSheet(
            sheetState = sheetState,
            selectedCategory = selectedCategory,
            selectedReceiver = selectedReceiver,
            receivers = receivers,
            fromDate = fromDate,
            toDate = toDate,
            amountRange = amountRange,
            onCategoryChange = { selectedCategory = it },
            onReceiverChange = { selectedReceiver = it },
            onDateRangeChange = { from, to -> fromDate = from; toDate = to },
            onAmountRangeChange = { amountRange = it },
            onDismiss = { showFilterSheet = false }
        )
    }
}

@Composable
private fun CategoryHeader(
    category: ExpenseCategory,
    count: Int,
    total: Double,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.expenseHeader)
            .clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(category::class.simpleName ?: "Other", style = MaterialTheme.typography.titleMedium)
            Text("$count items • ₹${total.toInt()}", style = MaterialTheme.typography.bodySmall)
        }
        Icon(
            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (expanded) "Collapse" else "Expand"
        )
    }
}

@Composable
private fun ExpenseItem(expense: ExpenseModel) {
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val todayStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    ListItem(
        headlineContent = { Text("₹${expense.amount ?: 0}") },
        supportingContent = expense.receiver?.let { { Text(it) } },
        trailingContent = {
            expense.time?.let {
                val format = if (it >= todayStart) timeFormat else dateFormat
                Text(format.format(Date(it)))
            }
        }
    )
}
