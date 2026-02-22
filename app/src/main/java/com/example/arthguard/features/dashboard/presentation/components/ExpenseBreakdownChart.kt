package com.example.arthguard.features.dashboard.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.arthguard.core.util.AppColors
import com.example.arthguard.features.dashboard.domain.model.DurationFilter
import com.example.arthguard.features.dashboard.domain.model.ExpenseCategory
import com.example.arthguard.features.dashboard.domain.model.ExpenseModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseBreakdownChart(
    expenses: List<ExpenseModel>,
    selectedDuration: DurationFilter,
    onDurationChange: (DurationFilter) -> Unit,
    onChartClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val filteredExpenses = remember(expenses, selectedDuration) {
        val startTime = selectedDuration.getStartTime()
        expenses.filter { (it.time ?: 0) >= startTime }
    }

    val categoryTotals = remember(filteredExpenses) {
        filteredExpenses
            .groupBy { it.category ?: ExpenseCategory.Other }
            .mapValues { entry -> entry.value.sumOf { it.amount ?: 0.0 } }
    }

    val total = categoryTotals.values.sum()

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.bgSecondary)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = selectedDuration.label,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DurationFilter.entries.forEach { duration ->
                        DropdownMenuItem(
                            text = { Text(duration.label) },
                            onClick = {
                                onDurationChange(duration)
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(48.dp))

            if (total > 0) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth().clickable { onChartClick() }
                ) {
                    DonutChart(categoryTotals = categoryTotals, total = total)
                    Text("₹${total.toInt()}", style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.height(16.dp))
                CategoryLegend(categoryTotals, total, onChartClick)
            } else {
                Text(
                    "No expenses for ${selectedDuration.label.lowercase()}",
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
private fun DonutChart(categoryTotals: Map<ExpenseCategory, Double>, total: Double) {
    Canvas(modifier = Modifier.size(160.dp)) {
        var startAngle = -90f
        categoryTotals.forEach { (category, amount) ->
            val sweep = (amount / total * 360).toFloat()
            drawArc(
                color = category.color,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                style = Stroke(width = 32.dp.toPx(), cap = StrokeCap.Butt)
            )
            startAngle += sweep
        }
    }
}

@Composable
private fun CategoryLegend(categoryTotals: Map<ExpenseCategory, Double>, total: Double, onClick: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        categoryTotals.forEach { (category, amount) ->
            val percentage = (amount / total * 100)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(Modifier.size(12.dp)) {
                    drawCircle(category.color)
                }
                Spacer(Modifier.width(8.dp))
                Text(category::class.simpleName ?: "Other", modifier = Modifier.weight(1f))
                Text("₹${amount.toInt()}   (${"%.1f".format(percentage)}%)")
            }
        }
    }
}

private val ExpenseCategory.color: Color
    get() = when (this) {
        ExpenseCategory.Food -> AppColors.expenseFood
        ExpenseCategory.Travelling -> AppColors.expenseTravelling
        ExpenseCategory.Groceries -> AppColors.expenseGroceries
        ExpenseCategory.Entertainment -> AppColors.expenseEntertainment
        ExpenseCategory.Shopping -> AppColors.expenseShopping
        ExpenseCategory.Bills -> AppColors.expenseBills
        ExpenseCategory.Other -> AppColors.expenseOther
    }
