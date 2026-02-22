package com.example.arthguard.features.dashboard.presentation.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.example.arthguard.core.util.AppColors
import com.example.arthguard.features.dashboard.domain.model.DurationFilter
import com.example.arthguard.features.dashboard.domain.model.ExpenseModel
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.ColumnCartesianLayerModel
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.common.Defaults
import com.patrykandpatrick.vico.core.common.Fill
import com.patrykandpatrick.vico.core.common.component.LineComponent
import com.patrykandpatrick.vico.core.common.data.ExtraStore

private val barColors = listOf(
    Color(0xFFE57373), Color(0xFF64B5F6), Color(0xFF81C784), Color(0xFFFFD54F),
    Color(0xFFBA68C8), Color(0xFF4DB6AC), Color(0xFFFF8A65), Color(0xFFA1887F)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopReceiversChart(
    expenses: List<ExpenseModel>,
    selectedDuration: DurationFilter,
    onDurationChange: (DurationFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val filteredExpenses = remember(expenses, selectedDuration) {
        val startTime = selectedDuration.getStartTime()
        expenses.filter { (it.time ?: 0) >= startTime }
    }

    val receiverTotals = remember(filteredExpenses) {
        filteredExpenses
            .filter { !it.receiver.isNullOrBlank() }
            .groupBy { it.receiver!! }
            .mapValues { entry -> entry.value.sumOf { it.amount ?: 0.0 } }
            .entries.sortedByDescending { it.value }
    }

    val labels = receiverTotals.map { it.key }
    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(receiverTotals) {
        if (receiverTotals.isNotEmpty()) {
            modelProducer.runTransaction {
                columnSeries {
                    series(receiverTotals.map { it.value })
                }
            }
        }
    }

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
                    label = { Text("Top Receivers") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DurationFilter.entries.forEach { duration ->
                        DropdownMenuItem(
                            text = { Text(duration.label) },
                            onClick = { onDurationChange(duration); expanded = false }
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            if (receiverTotals.isNotEmpty()) {
                val chartWidth = (receiverTotals.size * 100).coerceAtLeast(300)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    CartesianChartHost(
                        chart = rememberCartesianChart(
                            rememberColumnCartesianLayer(
                                columnProvider = remember(receiverTotals.size) {
                                    object : ColumnCartesianLayer.ColumnProvider {
                                        override fun getColumn(
                                            entry: ColumnCartesianLayerModel.Entry,
                                            seriesIndex: Int,
                                            extraStore: ExtraStore
                                        ): LineComponent {
                                            val colorIndex = entry.x.toInt() % barColors.size
                                            return LineComponent(
                                                fill = Fill(barColors[colorIndex].toArgb()),
                                                thicknessDp = Defaults.COLUMN_WIDTH
                                            )
                                        }
                                        override fun getWidestSeriesColumn(seriesIndex: Int, extraStore: ExtraStore): LineComponent {
                                            return LineComponent(Fill(barColors[0].toArgb()), Defaults.COLUMN_WIDTH)
                                        }
                                    }
                                }
                            ),
                            startAxis = VerticalAxis.rememberStart(),
                            bottomAxis = HorizontalAxis.rememberBottom(
                                valueFormatter = { _, x, _ ->
                                    val name = labels.getOrNull(x.toInt()) ?: " "
                                    if (name.length > 6) name.take(6) + "…" else name
                                }
                            )
                        ),
                        modelProducer = modelProducer,
                        modifier = Modifier.width(chartWidth.dp).height(200.dp)
                    )
                }
            } else {
                Text("No receiver data for ${selectedDuration.label.lowercase()}")
            }
        }
    }
}
