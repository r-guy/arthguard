package com.example.arthguard.features.dashboard.presentation.screen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.arthguard.core.util.AppColors
import com.example.arthguard.features.dashboard.presentation.components.AddExpenseBottomSheet
import com.example.arthguard.features.dashboard.presentation.components.BudgetCard
import com.example.arthguard.features.dashboard.presentation.components.ExpenseBreakdownChart
import com.example.arthguard.features.dashboard.presentation.components.ExpenseTrendChart
import com.example.arthguard.features.dashboard.presentation.components.SetBudgetBottomSheet
import com.example.arthguard.features.dashboard.presentation.components.TopCategoriesChart
import com.example.arthguard.features.dashboard.presentation.components.TopReceiversChart
import com.example.arthguard.features.dashboard.presentation.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToExpenseBreakup: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val budgetSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val currentBudget = uiState.getBudgetForDuration(uiState.budgetDuration)
    val totalSpent = remember(uiState.expenses, uiState.budgetDuration) {
        val startTime = uiState.budgetDuration.getStartTime()
        uiState.expenses.filter { (it.time ?: 0) >= startTime }.sumOf { it.amount ?: 0.0 }
    }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("ArthGuard") }) },
        floatingActionButton = {
            FloatingActionButton(
                containerColor = AppColors.bgTertiary,
                onClick = { viewModel.setShowAddExpenseSheet(true) },
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Expense")
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
                BudgetCard(
                    budget = currentBudget,
                    totalSpent = totalSpent,
                    duration = uiState.budgetDuration,
                    onDurationChange = { viewModel.setBudgetDuration(it) },
                    onSetBudget = { viewModel.setShowBudgetSheet(true) }
                )
                Spacer(Modifier.height(24.dp))

                Text("Expense Breakdown", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(8.dp))
                ExpenseBreakdownChart(
                    expenses = uiState.expenses,
                    selectedDuration = uiState.selectedDuration,
                    onDurationChange = { viewModel.setSelectedDuration(it) },
                    onChartClick = onNavigateToExpenseBreakup
                )
                Spacer(Modifier.height(24.dp))
                Text("Expense Trend", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(8.dp))
                ExpenseTrendChart(
                    expenses = uiState.expenses,
                    selectedDuration = uiState.trendDuration,
                    onDurationChange = { viewModel.setTrendDuration(it) }
                )
                Spacer(Modifier.height(24.dp))
                Text("Top Receivers", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(8.dp))
                TopReceiversChart(
                    expenses = uiState.expenses,
                    selectedDuration = uiState.receiversDuration,
                    onDurationChange = { viewModel.setReceiversDuration(it) }
                )
                Spacer(Modifier.height(24.dp))
                Text("Top Categories", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(8.dp))
                TopCategoriesChart(
                    expenses = uiState.expenses,
                    selectedDuration = uiState.categoriesDuration,
                    onDurationChange = { viewModel.setCategoriesDuration(it) }
                )
            }
        }
    }

    if (uiState.showAddExpenseSheet) {
        AddExpenseBottomSheet(
            sheetState = sheetState,
            onDismiss = { viewModel.setShowAddExpenseSheet(false) },
            onSave = { expense -> viewModel.saveExpense(expense) }
        )
    }

    if (uiState.showBudgetSheet) {
        SetBudgetBottomSheet(
            sheetState = budgetSheetState,
            duration = uiState.budgetDuration,
            currentAmount = currentBudget?.amount,
            onDismiss = { viewModel.setShowBudgetSheet(false) },
            onSave = { amount ->
                viewModel.saveBudget(uiState.budgetDuration, amount)
                viewModel.setShowBudgetSheet(false)
            }
        )
    }
}
