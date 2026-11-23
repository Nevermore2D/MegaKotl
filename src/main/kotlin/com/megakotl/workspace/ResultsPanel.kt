package com.megakotl.workspace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.megakotl.core.ExecutionMode
import com.megakotl.core.IntegrationBounds
import com.megakotl.core.TimedIntegral
import kotlin.math.abs

@Composable
internal fun ResultsPanel(
    state: WorkspaceState,
    bounds: IntegrationBounds,
    result: TimedIntegral?,
    comparison: TimedIntegral?,
    error: String?,
    calculating: Boolean
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Результаты", style = MaterialTheme.typography.titleMedium)
            when {
                calculating -> Text("Вычисление…", style = MaterialTheme.typography.bodyMedium)
                error != null -> Text(error, color = MaterialTheme.colorScheme.error)
                result == null -> Text("Задайте корректные параметры")
                else -> {
                    if (bounds.valid) {
                        Text("Границы: [${shortNumber(requireNotNull(bounds.a))}; ${shortNumber(requireNotNull(bounds.b))}]", style = MaterialTheme.typography.bodySmall)
                    }
                    Text("∫ f(x) dx ≈ ${shortNumber(result.values.value)}", style = MaterialTheme.typography.titleLarge)
                    result.values.lower?.let { Text("Нижняя выборочная сумма: ${shortNumber(it)}") }
                    result.values.upper?.let { Text("Верхняя выборочная сумма: ${shortNumber(it)}") }
                    if (result.values.lower != null && result.values.upper != null) {
                        Text("Разность: ${shortNumber(result.values.upper - result.values.lower)}")
                        Text("Это оценки экстремумов по 33 точкам на каждом отрезке, не строгие суммы Дарбу.", style = MaterialTheme.typography.bodySmall)
                    }
                    Text("${state.execution.title}: ${shortNumber(result.millis)} мс (${result.workers} задач)", style = MaterialTheme.typography.bodySmall)
                    if (comparison != null) {
                        val otherMode = if (state.execution == ExecutionMode.SEQUENTIAL) ExecutionMode.PARALLEL else ExecutionMode.SEQUENTIAL
                        Text("${otherMode.title}: ${shortNumber(comparison.millis)} мс (${comparison.workers} задач)", style = MaterialTheme.typography.bodySmall)
                        Text("Разница результатов: ${shortNumber(abs(result.values.value - comparison.values.value))}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
