package com.example.arthguard.features.dashboard.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.arthguard.core.util.AppColors
import com.example.arthguard.features.dashboard.domain.model.ExpenseModel
import com.example.arthguard.features.dashboard.domain.model.TrendDuration
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries

enum class TrendMode(val label: String) {
    SPECIFIC("Specific"),
    CUMULATIVE("Cumulative")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseTrendChart(
    expenses: List<ExpenseModel>,
    selectedDuration: TrendDuration,
    onDurationChange: (TrendDuration) -> Unit,
    modifier: Modifier = Modifier
) {
    var durationExpanded by remember { mutableStateOf(false) }
    var modeExpanded by remember { mutableStateOf(false) }
    var selectedMode by remember { mutableStateOf(TrendMode.SPECIFIC) }
    
    val dataPoints = remember(expenses, selectedDuration) { selectedDuration.getDataPoints(expenses) }
    val modelProducer = remember { CartesianChartModelProducer() }
    val labels = remember(dataPoints) { dataPoints.map { it.first } }
    
    LaunchedEffect(dataPoints, selectedMode) {
        val values = dataPoints.map { it.second }
        val chartValues = when (selectedMode) {
            TrendMode.SPECIFIC -> values
            TrendMode.CUMULATIVE -> values.runningReduce { acc, d -> acc + d }
        }
        modelProducer.runTransaction { lineSeries { series(chartValues.ifEmpty { listOf(0.0) }) } }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.bgSecondary)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(
                    expanded = durationExpanded,
                    onExpandedChange = { durationExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedDuration.label,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(durationExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = durationExpanded, onDismissRequest = { durationExpanded = false }) {
                        TrendDuration.entries.forEach { duration ->
                            DropdownMenuItem(
                                text = { Text(duration.label) },
                                onClick = { onDurationChange(duration); durationExpanded = false }
                            )
                        }
                    }
                }
                ExposedDropdownMenuBox(
                    expanded = modeExpanded,
                    onExpandedChange = { modeExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedMode.label,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(modeExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = modeExpanded, onDismissRequest = { modeExpanded = false }) {
                        TrendMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode.label) },
                                onClick = { selectedMode = mode; modeExpanded = false }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            CartesianChartHost(
                chart = rememberCartesianChart(
                    rememberLineCartesianLayer(),
                    startAxis = VerticalAxis.rememberStart(),
                    bottomAxis = HorizontalAxis.rememberBottom(
                        valueFormatter = { _, x, _ -> labels.getOrNull(x.toInt()) ?: " " }
                    )
                ),
                modelProducer = modelProducer,
                modifier = Modifier.fillMaxWidth().height(200.dp)
            )
        }
    }
}
