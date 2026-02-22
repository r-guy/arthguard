package com.example.arthguard.features.expense_breakup.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.arthguard.core.util.AppColors
import com.example.arthguard.core.util.Constants.amountLabels
import com.example.arthguard.core.util.ui.AppBottomSheet
import com.example.arthguard.features.dashboard.domain.model.ExpenseCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseFilterBottomSheet(
    sheetState: SheetState,
    selectedCategory: ExpenseCategory?,
    selectedReceiver: String?,
    receivers: List<String>,
    fromDate: Long?,
    toDate: Long?,
    amountRange: ClosedFloatingPointRange<Float>,
    onCategoryChange: (ExpenseCategory?) -> Unit,
    onReceiverChange: (String?) -> Unit,
    onDateRangeChange: (Long?, Long?) -> Unit,
    onAmountRangeChange: (ClosedFloatingPointRange<Float>) -> Unit,
    onDismiss: () -> Unit
) {
    var categoryExpanded by remember { mutableStateOf(false) }
    var receiverExpanded by remember { mutableStateOf(false) }
    var localCategory by remember(selectedCategory) { mutableStateOf(selectedCategory) }
    var localReceiver by remember(selectedReceiver) { mutableStateOf(selectedReceiver) }
    var localAmountRange by remember(amountRange) { mutableStateOf(amountRange) }
    val dateRangeState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = fromDate,
        initialSelectedEndDateMillis = toDate
    )

    var categorySectionExpanded by remember { mutableStateOf(true) }
    var receiverSectionExpanded by remember { mutableStateOf(false) }
    var dateSectionExpanded by remember { mutableStateOf(false) }
    var amountSectionExpanded by remember { mutableStateOf(false) }

    val categories = listOf(null) + listOf(
        ExpenseCategory.Food, ExpenseCategory.Travelling, ExpenseCategory.Groceries,
        ExpenseCategory.Entertainment, ExpenseCategory.Shopping, ExpenseCategory.Bills, ExpenseCategory.Other
    )

    AppBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        isKeyBoardEnabled = false,
        horizontalPadding = 4.dp,
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Filters", style = MaterialTheme.typography.titleMedium.copy(color = AppColors.textPrimary))

                // Category Section
                ExpandableSection(
                    title = "Category",
                    expanded = categorySectionExpanded,
                    onToggle = { categorySectionExpanded = !categorySectionExpanded }
                ) {
                    ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = it }) {
                        OutlinedTextField(
                            value = localCategory?.let { it::class.simpleName } ?: "All Categories",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = AppColors.textPrimary)
                        )
                        ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat?.let { it::class.simpleName } ?: "All Categories", color = AppColors.textPrimary) },
                                    onClick = {
                                        localCategory = cat
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Receiver Section
                if (receivers.isNotEmpty()) {
                    ExpandableSection(
                        title = "Receiver",
                        expanded = receiverSectionExpanded,
                        onToggle = { receiverSectionExpanded = !receiverSectionExpanded }
                    ) {
                        ExposedDropdownMenuBox(expanded = receiverExpanded, onExpandedChange = { receiverExpanded = it }) {
                            OutlinedTextField(
                                value = localReceiver ?: "All Receivers",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(receiverExpanded) },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth(),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = AppColors.textPrimary)
                            )
                            ExposedDropdownMenu(expanded = receiverExpanded, onDismissRequest = { receiverExpanded = false }) {
                                DropdownMenuItem(
                                    text = { Text("All Receivers", color = AppColors.textPrimary) },
                                    onClick = { localReceiver = null; receiverExpanded = false }
                                )
                                receivers.forEach { receiver ->
                                    DropdownMenuItem(
                                        text = { Text(receiver, color = AppColors.textPrimary) },
                                        onClick = { localReceiver = receiver; receiverExpanded = false }
                                    )
                                }
                            }
                        }
                    }
                }

                // Date Range Section
                ExpandableSection(
                    title = "Date Range",
                    expanded = dateSectionExpanded,
                    onToggle = { dateSectionExpanded = !dateSectionExpanded }
                ) {
                    DateRangePicker(
                        state = dateRangeState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp),
                        title = null,
                        headline = null,
                        showModeToggle = false
                    )
                }

                // Amount Section
                ExpandableSection(
                    title = "Amount: ${amountLabels[localAmountRange.start.toInt()]} - ${amountLabels[localAmountRange.endInclusive.toInt()]}",
                    expanded = amountSectionExpanded,
                    onToggle = { amountSectionExpanded = !amountSectionExpanded }
                ) {
                    RangeSlider(
                        value = localAmountRange,
                        onValueChange = { localAmountRange = it },
                        valueRange = 0f..11f,
                        steps = 10,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        onCategoryChange(localCategory)
                        onReceiverChange(localReceiver)
                        onDateRangeChange(dateRangeState.selectedStartDateMillis, dateRangeState.selectedEndDateMillis)
                        onAmountRangeChange(localAmountRange)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Apply Filters", color = AppColors.textInverse)
                }
            }
        }
    )
}

@Composable
private fun ExpandableSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall.copy(color = AppColors.textPrimary))
            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = AppColors.textPrimary
            )
        }
        AnimatedVisibility(visible = expanded) {
            content()
        }
    }
}
