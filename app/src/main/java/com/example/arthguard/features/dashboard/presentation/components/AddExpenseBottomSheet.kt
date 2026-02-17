package com.example.arthguard.features.dashboard.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.arthguard.core.util.AppColors
import com.example.arthguard.core.util.ui.AppBottomSheet
import com.example.arthguard.features.dashboard.domain.model.ExpenseCategory
import com.example.arthguard.features.dashboard.domain.model.ExpenseModel

private val categories = listOf(
    ExpenseCategory.Food,
    ExpenseCategory.Travelling,
    ExpenseCategory.Groceries,
    ExpenseCategory.Entertainment,
    ExpenseCategory.Shopping,
    ExpenseCategory.Bills,
    ExpenseCategory.Other
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseBottomSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSave: (ExpenseModel) -> Unit
) {
    ExpenseBottomSheet(
        sheetState = sheetState,
        onDismiss = onDismiss,
        title = "Add Expense",
        buttonText = "Save",
        onSubmit = onSave
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExpenseBottomSheet(
    sheetState: SheetState,
    expense: ExpenseModel,
    onDismiss: () -> Unit,
    onUpdate: (ExpenseModel) -> Unit
) {
    ExpenseBottomSheet(
        sheetState = sheetState,
        expense = expense,
        onDismiss = onDismiss,
        title = "Edit Expense",
        buttonText = "Update",
        onSubmit = onUpdate
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseBottomSheet(
    sheetState: SheetState,
    expense: ExpenseModel? = null,
    onDismiss: () -> Unit,
    title: String,
    buttonText: String,
    onSubmit: (ExpenseModel) -> Unit
) {
    var amount by remember { mutableStateOf(expense?.amount?.toString() ?: "") }
    var receiver by remember { mutableStateOf(expense?.receiver ?: "") }
    var selectedCategory by remember { mutableStateOf(expense?.category) }
    var expanded by remember { mutableStateOf(false) }

    AppBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        isKeyBoardEnabled = true,
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = AppColors.textPrimary,
                    )
                )
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = receiver,
                    onValueChange = { receiver = it },
                    label = { Text("Receiver (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.toString()?.substringAfterLast(".") ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.toString().substringAfterLast(".")) },
                                onClick = {
                                    selectedCategory = category
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = {
                        onSubmit(
                            ExpenseModel(
                                id = expense?.id,
                                amount = amount.toDoubleOrNull(),
                                category = selectedCategory,
                                receiver = receiver.ifBlank { null },
                                time = expense?.time ?: System.currentTimeMillis(),
                                type = expense?.type ?: "DEBIT",
                                source = expense?.source ?: "MANUAL",
                                rawMessage = expense?.rawMessage,
                                sender = expense?.sender
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(buttonText)
                }
            }
        }
    )
}
