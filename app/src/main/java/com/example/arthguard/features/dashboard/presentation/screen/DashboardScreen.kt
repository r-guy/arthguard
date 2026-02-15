package com.example.arthguard.features.dashboard.presentation.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.arthguard.core.data.local.AppDatabase
import com.example.arthguard.features.dashboard.data.repository.ExpenseRepositoryImpl
import com.example.arthguard.features.dashboard.domain.model.DurationFilter
import com.example.arthguard.features.dashboard.domain.model.TrendDuration
import com.example.arthguard.features.dashboard.presentation.components.AddExpenseBottomSheet
import com.example.arthguard.features.dashboard.presentation.components.ExpenseBreakdownChart
import com.example.arthguard.features.dashboard.presentation.components.ExpenseTrendChart
import com.example.arthguard.features.dashboard.presentation.components.TopReceiversChart
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    onNavigateToExpenseBreakup: () -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember { ExpenseRepositoryImpl(AppDatabase.getInstance(context).expenseDao()) }
    val expenses by repository.getAllExpenses().collectAsState(initial = emptyList())

    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var selectedDuration by remember { mutableStateOf(DurationFilter.THIS_MONTH) }
    var trendDuration by remember { mutableStateOf(TrendDuration.WEEK) }
    var receiversDuration by remember { mutableStateOf(DurationFilter.THIS_MONTH) }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Dashboard") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showSheet = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 16.dp
            )
        ) {
            item {
                ExpenseBreakdownChart(
                    expenses = expenses,
                    selectedDuration = selectedDuration,
                    onDurationChange = { selectedDuration = it },
                    onChartClick = onNavigateToExpenseBreakup
                )
                Spacer(Modifier.height(16.dp))
                ExpenseTrendChart(
                    expenses = expenses,
                    selectedDuration = trendDuration,
                    onDurationChange = { trendDuration = it }
                )
                Spacer(Modifier.height(16.dp))
                TopReceiversChart(
                    expenses = expenses,
                    selectedDuration = receiversDuration,
                    onDurationChange = { receiversDuration = it }
                )
            }
        }

        if (showSheet) {
            AddExpenseBottomSheet(
                sheetState = sheetState,
                onDismiss = { showSheet = false },
                onSave = { expense ->
                    scope.launch {
                        repository.saveExpense(expense)
                        showSheet = false
                    }
                }
            )
        }
    }
}
