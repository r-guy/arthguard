package com.example.arthguard.features.dashboard.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.arthguard.core.util.AppColors
import com.example.arthguard.core.util.ui.AppBottomSheet
import com.example.arthguard.features.dashboard.data.local.BudgetEntity
import com.example.arthguard.features.dashboard.domain.model.DurationFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetCard(
    budget: BudgetEntity?,
    totalSpent: Double,
    duration: DurationFilter,
    onDurationChange: (DurationFilter) -> Unit,
    onSetBudget: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.bgSecondary)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp,)
                .fillMaxWidth(),
        ) {
            Text(
                if (budget != null) "Edit" else "Set Budget",
                modifier = Modifier
                    .padding(bottom = 12.dp,)
                    .clickable(
                        onClick = onSetBudget,
                    )
            )
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = duration.label,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DurationFilter.entries.forEach { d ->
                        DropdownMenuItem(
                            text = { Text(d.label) },
                            onClick = {
                                onDurationChange(d)
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (budget != null) {
                val percent = if (budget.amount > 0) (totalSpent / budget.amount).coerceAtMost(1.0) else 0.0
                val percentDisplay = (percent * 100).toInt()
                val isOverBudget = totalSpent > budget.amount

                LinearProgressIndicator(
                    progress = { percent.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = if (isOverBudget) AppColors.red400 else AppColors.green400,
                    trackColor = AppColors.gray500
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("₹%.0f spent".format(totalSpent), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "₹%.0f budget (%d%%)".format(budget.amount, percentDisplay),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (isOverBudget) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Over budget by ₹%.0f".format(totalSpent - budget.amount),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.red400,
                    )
                }
            } else {
                Text(
                    "₹%.0f spent".format(totalSpent),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "No budget set for ${duration.label.lowercase()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.textSecondary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetBudgetBottomSheet(
    sheetState: SheetState,
    duration: DurationFilter,
    currentAmount: Double?,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var amount by remember(currentAmount) { mutableStateOf(currentAmount?.toString() ?: "") }

    AppBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        isKeyBoardEnabled = true,
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Set ${duration.label} Budget", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Budget Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    prefix = { Text("₹") }
                )
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(
                        onClick = { amount.toDoubleOrNull()?.let { onSave(it) } },
                        enabled = amount.toDoubleOrNull() != null && amount.toDoubleOrNull()!! > 0
                    ) { Text("Save") }
                }
            }
        }
    )
}
