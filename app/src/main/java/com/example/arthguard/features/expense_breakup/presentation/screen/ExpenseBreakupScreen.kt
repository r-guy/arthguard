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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.arthguard.core.data.local.AppDatabase
import com.example.arthguard.core.util.AppColors
import com.example.arthguard.core.util.Constants.amountStops
import com.example.arthguard.core.util.ui.ConfirmDeleteBottomSheet
import com.example.arthguard.features.dashboard.data.local.ExpenseEntity
import com.example.arthguard.features.dashboard.data.repository.ExpenseRepositoryImpl
import com.example.arthguard.features.dashboard.domain.model.ExpenseCategory
import com.example.arthguard.features.dashboard.domain.model.ExpenseModel
import com.example.arthguard.features.dashboard.presentation.components.EditExpenseBottomSheet
import com.example.arthguard.features.expense_breakup.presentation.components.ExpenseFilterBottomSheet
import com.example.arthguard.features.sms_expense.data.SmsReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    val dao = remember { AppDatabase.getInstance(context).expenseDao() }
    val repository = remember { ExpenseRepositoryImpl(dao) }
    val expenses by repository.getAllExpenses().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var selectedCategory by remember { mutableStateOf<ExpenseCategory?>(null) }
    var selectedReceiver by remember { mutableStateOf<String?>(null) }
    var fromDate by remember { mutableStateOf<Long?>(null) }
    var toDate by remember { mutableStateOf<Long?>(null) }
    var amountRange by remember { mutableStateOf(0f..11f) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var expenseToEdit by remember { mutableStateOf<ExpenseModel?>(null) }
    var expenseToDelete by remember { mutableStateOf<ExpenseModel?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val editSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val deleteSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val receivers by remember(expenses) {
        derivedStateOf { expenses.mapNotNull { it.receiver }.distinct() }
    }

    val filteredExpenses by remember(expenses, selectedCategory, selectedReceiver, fromDate, toDate, amountRange, searchQuery) {
        derivedStateOf {
            expenses.filter { expense ->
                val categoryMatch = selectedCategory == null || expense.category == selectedCategory
                val receiverMatch = selectedReceiver == null || expense.receiver == selectedReceiver
                val fromMatch = fromDate == null || (expense.time ?: 0) >= fromDate!!
                val toMatch = toDate == null || (expense.time ?: 0) <= toDate!!
                val minAmount = amountStops[amountRange.start.toInt()]
                val maxAmount = amountStops[amountRange.endInclusive.toInt()]
                val amountMatch = (expense.amount ?: 0.0) >= minAmount && (expense.amount ?: 0.0) <= maxAmount
                val searchMatch = searchQuery.isBlank() || 
                    expense.receiver?.contains(searchQuery, ignoreCase = true) == true ||
                    expense.category?.let { it::class.simpleName?.contains(searchQuery, ignoreCase = true) } == true ||
                    expense.amount?.toString()?.contains(searchQuery) == true
                categoryMatch && receiverMatch && fromMatch && toMatch && amountMatch && searchMatch
            }
        }
    }

    val groupedExpenses by remember(filteredExpenses) {
        derivedStateOf {
            filteredExpenses.groupBy { it.category ?: ExpenseCategory.Other }
        }
    }

    val expandedCategories = remember { mutableStateMapOf<ExpenseCategory, Boolean>() }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            focusRequester.requestFocus()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            if (isSearchActive) {
                SearchBar(
                    inputField = {
                        SearchBarDefaults.InputField(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            onSearch = { },
                            expanded = false,
                            onExpandedChange = { },
                            modifier = Modifier.focusRequester(focusRequester),
                            placeholder = { Text("Search expenses...") },
                            leadingIcon = {
                                IconButton(
                                    onClick = {
                                        isSearchActive = false
                                        searchQuery = ""
                                    }
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Close search",
                                    )
                                }
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(size = 64.dp,),)
                                            .background(color = AppColors.red300,),
                                        onClick = { searchQuery = "" }
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Clear",
                                        )
                                    }
                                }
                            }
                        )
                    },
                    expanded = false,
                    onExpandedChange = { },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 8.dp)
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
                        IconButton(onClick = {
                            scope.launch(Dispatchers.IO) {
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
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                        IconButton(onClick = { showFilterSheet = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter")
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (!isSearchActive) {
                FloatingActionButton(onClick = { isSearchActive = true }) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
            }
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
                        ExpenseItem(
                            expense = expense,
                            onEditClick = { expenseToEdit = expense },
                            onDeleteClick = { expenseToDelete = expense }
                        )
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        ExpenseFilterBottomSheet(
            sheetState = filterSheetState,
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

    expenseToEdit?.let { expense ->
        EditExpenseBottomSheet(
            sheetState = editSheetState,
            expense = expense,
            onDismiss = { expenseToEdit = null },
            onUpdate = { updated ->
                updated.id?.toLongOrNull()?.let { id ->
                    scope.launch {
                        dao.update(
                            id = id,
                            amount = updated.amount,
                            receiver = updated.receiver,
                            category = updated.category?.let { it::class.simpleName }
                        )
                    }
                }
                expenseToEdit = null
            }
        )
    }

    expenseToDelete?.let { expense ->
        ConfirmDeleteBottomSheet(
            sheetState = deleteSheetState,
            onDismiss = { expenseToDelete = null },
            onConfirm = {
                expense.id?.toLongOrNull()?.let { id ->
                    scope.launch { dao.delete(id) }
                }
                expenseToDelete = null
            }
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
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    )
}
