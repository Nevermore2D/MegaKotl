package com.megakotl.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.megakotl.core.CurveDefinition
import com.megakotl.core.ExecutionMode
import com.megakotl.core.resolveIntegrationBounds
import com.megakotl.core.TimedIntegral
import com.megakotl.core.runIntegral
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

@Composable
fun WorkspaceScreen() {
    val state = remember { WorkspaceState() }
    val definition = CurveDefinition(state.kind, state.points.toList(), state.formula)
    val evaluator = remember(definition) { definition.newEvaluator() }

    var calculation by remember { mutableStateOf<TimedIntegral?>(null) }
    var comparison by remember { mutableStateOf<TimedIntegral?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var calculating by remember { mutableStateOf(false) }

    val n = state.n.toIntOrNull()
    val xMin = parseDouble(state.xMin)
    val xMax = parseDouble(state.xMax)
    val validPlotRange = xMin != null && xMax != null && xMin < xMax
    val bounds = resolveIntegrationBounds(
        state.integrateFullRange, parseDouble(state.a), parseDouble(state.b), xMin, xMax,
        definition.integrationDomain()
    )
    val a = bounds.a
    val b = bounds.b

    LaunchedEffect(definition, state.method, state.execution, state.compare, state.integrateFullRange, a, b, n) {
        calculation = null
        comparison = null
        error = null
        calculating = false
        val inputError = definition.validationError()
            ?: if (state.integrateFullRange && !validPlotRange) {
                "Для интегрирования по диапазону построения задайте X от < X до"
            } else if (state.integrateFullRange && !bounds.valid && definition.integrationDomain() != null) {
                "Диапазон построения не пересекается с областью сплайна между крайними узлами"
            } else if (a == null || b == null || !bounds.valid) {
                "Для интегрирования задайте конечные a < b"
            } else if (n == null || n !in 1..20_000) {
                "Количество разбиений n должно быть от 1 до 20000"
            } else {
                definition.domainError(a, b)
            }
        if (inputError != null) {
            error = inputError
            return@LaunchedEffect
        }
        delay(180) // do not launch a heavy calculation on every intermediate keystroke
        calculating = true
        try {
            val lower = requireNotNull(a)
            val upper = requireNotNull(b)
            val partitions = requireNotNull(n)
            calculation = runIntegral(definition, lower, upper, partitions, state.method, state.execution)
            if (state.compare) {
                val alternate = if (state.execution == ExecutionMode.SEQUENTIAL) ExecutionMode.PARALLEL
                    else ExecutionMode.SEQUENTIAL
                comparison = runIntegral(definition, lower, upper, partitions, state.method, alternate)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (exception: Exception) {
            error = exception.message ?: "Не удалось вычислить интеграл"
        } finally {
            calculating = false
        }
    }

    Row(Modifier.fillMaxSize().background(Color(0xFFF4F6FA))) {
        ControlsPanel(
            state = state,
            bounds = bounds,
            result = calculation,
            comparison = comparison,
            error = error,
            calculating = calculating,
            modifier = Modifier.width(350.dp).fillMaxHeight()
        )
        Column(Modifier.weight(1f).fillMaxHeight()) {
            Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("MegaKotl", style = MaterialTheme.typography.titleLarge)
                    Text("${state.kind.title}  ·  ${state.method.title}", style = MaterialTheme.typography.bodySmall)
                }
            }
            HorizontalDivider()
            Box(Modifier.weight(1f).fillMaxWidth().padding(10.dp)) {
                UnifiedGraph(
                    state = state,
                    evaluator = evaluator,
                    plotMin = if (validPlotRange) xMin else null,
                    plotMax = if (validPlotRange) xMax else null,
                    a = a,
                    b = b,
                    n = n
                )
            }
            Surface(Modifier.fillMaxWidth(), tonalElevation = 2.dp) {
                Text(state.status, Modifier.padding(horizontal = 14.dp, vertical = 9.dp), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
