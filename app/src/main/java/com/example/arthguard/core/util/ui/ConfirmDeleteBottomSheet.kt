package com.example.arthguard.core.util.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.arthguard.core.util.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmDeleteBottomSheet(
    sheetState: SheetState,
    title: String = "Delete Expense",
    message: String = "Are you sure you want to delete this expense?",
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AppBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = AppColors.textPrimary,
                    ),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = AppColors.textSecondary,
                    ),
                )
                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                }
            }
        }
    )
}
