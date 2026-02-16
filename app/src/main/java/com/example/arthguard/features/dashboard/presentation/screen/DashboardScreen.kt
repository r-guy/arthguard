package com.example.arthguard.features.dashboard.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.arthguard.core.data.local.AppDatabase
import com.example.arthguard.core.util.AppColors
import com.example.arthguard.features.dashboard.data.local.BudgetEntity
import com.example.arthguard.features.dashboard.data.repository.ExpenseRepositoryImpl
import com.example.arthguard.features.dashboard.domain.model.DurationFilter
import com.example.arthguard.features.dashboard.domain.model.TrendDuration
import com.example.arthguard.features.dashboard.presentation.components.AddBudgetButton
import com.example.arthguard.features.dashboard.presentation.components.AddExpenseBottomSheet
import com.example.arthguard.features.dashboard.presentation.components.ExpenseBreakdownChart
import com.example.arthguard.features.dashboard.presentation.components.ExpenseTrendChart
import com.example.arthguard.features.dashboard.presentation.components.TopCategoriesChart
import com.example.arthguard.features.dashboard.presentation.components.TopReceiversChart
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    onNavigateToExpenseBreakup: () -> Unit = {}
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val repository = remember { ExpenseRepositoryImpl(db.expenseDao()) }
    val expenses by repository.getAllExpenses().collectAsState(initial = emptyList())

    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val budgetSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var selectedDuration by remember { mutableStateOf(DurationFilter.THIS_MONTH) }
    var trendDuration by remember { mutableStateOf(TrendDuration.WEEK) }
    var receiversDuration by remember { mutableStateOf(DurationFilter.THIS_MONTH) }
    var categoriesDuration by remember { mutableStateOf(DurationFilter.THIS_MONTH) }

    val budgets by db.budgetDao().getAllBudgets().collectAsState(initial = emptyList())
    val hasBudget = budgets.isNotEmpty()
    
    val activeBudget = budgets.firstOrNull()
    val activeDuration = activeBudget?.let { DurationFilter.valueOf(it.duration) }
    val totalSpent = remember(expenses, activeDuration) {
        val startTime = activeDuration?.getStartTime() ?: 0L
        expenses.filter { (it.time ?: 0) >= startTime }.sumOf { it.amount ?: 0.0 }
    }
    val budgetPercent = activeBudget?.let { if (it.amount > 0) (totalSpent / it.amount * 100) else 0.0 } ?: 0.0

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
                if (activeBudget != null) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(shape = RoundedCornerShape(size = 32.dp))
                            .background(color = AppColors.white),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "₹%.0f / ₹%.0f".format(totalSpent, activeBudget.amount),
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = AppColors.textInverse,
                            ),
                            modifier = Modifier
                                .weight(0.7f)
                                .fillMaxHeight()
                                .clip(shape = RoundedCornerShape(size = 32.dp))
                                .background(color = AppColors.gray100)
                                .padding(vertical = 8.dp),
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            "(%.0f%%)".format(budgetPercent),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                color = AppColors.textInverse,
                                fontWeight = FontWeight.W800,
                            ),
                            modifier = Modifier
                                .weight(0.3f)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                } else {
                    AddBudgetButton(
                        onSaveBudget = { duration, amount ->
                            scope.launch {
                                db.budgetDao().saveBudget(
                                    BudgetEntity(
                                        duration = duration.name,
                                        amount = amount,
                                        lastBudget = null,
                                        updatedAt = System.currentTimeMillis()
                                    )
                                )
                            }
                        },
                        sheetState = budgetSheetState
                    )
                    Spacer(Modifier.height(24.dp))
                }
                Text("Expense Breakdown", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(8.dp))
                ExpenseBreakdownChart(
                    expenses = expenses,
                    selectedDuration = selectedDuration,
                    onDurationChange = { selectedDuration = it },
                    onChartClick = onNavigateToExpenseBreakup
                )
                Spacer(Modifier.height(24.dp))
                Text("Expense Trend", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(8.dp))
                ExpenseTrendChart(
                    expenses = expenses,
                    selectedDuration = trendDuration,
                    onDurationChange = { trendDuration = it }
                )
                Spacer(Modifier.height(24.dp))
                Text("Top Receivers", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(8.dp))
                TopReceiversChart(
                    expenses = expenses,
                    selectedDuration = receiversDuration,
                    onDurationChange = { receiversDuration = it }
                )
                Spacer(Modifier.height(24.dp))
                Text("Top Categories", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(8.dp))
                TopCategoriesChart(
                    expenses = expenses,
                    selectedDuration = categoriesDuration,
                    onDurationChange = { categoriesDuration = it }
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
