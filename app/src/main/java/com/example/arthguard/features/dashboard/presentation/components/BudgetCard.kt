package com.example.arthguard.features.dashboard.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.arthguard.core.util.AppColors
import com.example.arthguard.features.dashboard.domain.model.DurationFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBudgetButton(
    onSaveBudget: (DurationFilter, Double) -> Unit,
    sheetState: SheetState,
    modifier: Modifier = Modifier
) {
    var showSheet by remember { mutableStateOf(false) }

    ElevatedButton(
        modifier = modifier
            .fillMaxWidth(),
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = AppColors.white,
        ),
        onClick = { showSheet = true },
    ) {
        Text(
            "Add Budget",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = AppColors.textInverse,
            )
        )
    }

    if (showSheet) {
        SetBudgetSheet(
            sheetState = sheetState,
            onDismiss = { showSheet = false },
            onSave = { duration, amount -> onSaveBudget(duration, amount); showSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetBudgetSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSave: (DurationFilter, Double) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf(DurationFilter.THIS_MONTH) }
    var expanded by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(16.dp)) {
            Text("Set Budget", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = duration.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Duration") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DurationFilter.entries.forEach {
                        DropdownMenuItem(text = { Text(it.label) }, onClick = { duration = it; expanded = false })
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Budget Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                TextButton(onClick = { amount.toDoubleOrNull()?.let { onSave(duration, it) } }) { Text("Save") }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
