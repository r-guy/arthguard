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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.arthguard.core.util.AppColors
import com.example.arthguard.core.util.ui.ConfirmDeleteBottomSheet
import com.example.arthguard.features.dashboard.domain.model.ExpenseCategory
import com.example.arthguard.features.dashboard.domain.model.ExpenseModel
import com.example.arthguard.features.dashboard.presentation.components.EditExpenseBottomSheet
import com.example.arthguard.features.expense_breakup.presentation.components.ExpenseFilterBottomSheet
import com.example.arthguard.features.expense_breakup.presentation.viewmodel.ExpenseBreakupViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseBreakupScreen(
    modifier: Modifier = Modifier,
    viewModel: ExpenseBreakupViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val deleteSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val expandedCategories = remember { mutableStateMapOf<ExpenseCategory, Boolean>() }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(uiState.isSearchActive) {
        if (uiState.isSearchActive) focusRequester.requestFocus()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            if (uiState.isSearchActive) {
                SearchBar(
                    inputField = {
                        SearchBarDefaults.InputField(
                            query = uiState.searchQuery,
                            onQueryChange = { viewModel.setSearchQuery(it) },
                            onSearch = { },
                            expanded = false,
                            onExpandedChange = { },
                            modifier = Modifier.focusRequester(focusRequester),
                            placeholder = { Text("Search expenses...") },
                            leadingIcon = {
                                IconButton(onClick = { viewModel.setSearchActive(false) }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close search")
                                }
                            },
                            trailingIcon = {
                                if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(size = 64.dp))
                                            .background(color = AppColors.red300),
                                        onClick = { viewModel.setSearchQuery("") }
                                    ) {
                                        Icon(
                                            Icons.Rounded.Close,
                                            contentDescription = "Clear",
                                            tint = AppColors.white,
                                        )
                                    }
                                }
                            }
                        )
                    },
                    expanded = false,
                    onExpandedChange = { },
                    modifier = Modifier.fillMaxWidth().padding(all = 8.dp)
                ) {}
            } else {
                TopAppBar(
                    title = { Text("Expense Breakup") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.refreshFromSms(context) }) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Refresh")
                        }
                        IconButton(onClick = { viewModel.setShowFilterSheet(true) }) {
                            Icon(Icons.Rounded.FilterList, contentDescription = "Filter")
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (!uiState.isSearchActive) {
                FloatingActionButton(
                    containerColor = AppColors.bgTertiary,
                    onClick = { viewModel.setSearchActive(true) },
                ) {
                    Icon(Icons.Rounded.Search, contentDescription = "Search")
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            uiState.groupedExpenses.forEach { (category, categoryExpenses) ->
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
                        ExpenseItem(
                            expense = expense,
                            onEditClick = { viewModel.setExpenseToEdit(expense) },
                            onDeleteClick = { viewModel.setExpenseToDelete(expense) }
                        )
                    }
                }
            }
        }
    }

    if (uiState.showFilterSheet) {
        ExpenseFilterBottomSheet(
            sheetState = filterSheetState,
            selectedCategory = uiState.selectedCategory,
            selectedReceiver = uiState.selectedReceiver,
            receivers = uiState.receivers,
            fromDate = uiState.fromDate,
            toDate = uiState.toDate,
            amountRange = uiState.amountRange,
            onCategoryChange = { viewModel.setSelectedCategory(it) },
            onReceiverChange = { viewModel.setSelectedReceiver(it) },
            onDateRangeChange = { from, to -> viewModel.setDateRange(from, to) },
            onAmountRangeChange = { viewModel.setAmountRange(it) },
            onDismiss = { viewModel.setShowFilterSheet(false) }
        )
    }

    uiState.expenseToEdit?.let { expense ->
        EditExpenseBottomSheet(
            sheetState = editSheetState,
            expense = expense,
            onDismiss = { viewModel.setExpenseToEdit(null) },
            onUpdate = { viewModel.updateExpense(it) }
        )
    }

    uiState.expenseToDelete?.let { expense ->
        ConfirmDeleteBottomSheet(
            sheetState = deleteSheetState,
            onDismiss = { viewModel.setExpenseToDelete(null) },
            onConfirm = { viewModel.deleteExpense(expense) }
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
            .background(AppColors.bgSecondary)
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
            imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
            contentDescription = if (expanded) "Collapse" else "Expand"
        )
    }
}

@Composable
private fun ExpenseItem(
    expense: ExpenseModel,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                expense.time?.let {
                    val format = if (it >= todayStart) timeFormat else dateFormat
                    Text(format.format(Date(it)))
                }
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    )
}
