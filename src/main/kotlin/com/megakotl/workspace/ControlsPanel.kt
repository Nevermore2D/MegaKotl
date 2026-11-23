package com.megakotl.workspace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.megakotl.core.CurveKind
import com.megakotl.core.ExecutionMode
import com.megakotl.core.GraphPoint
import com.megakotl.core.IntegralMethod
import com.megakotl.core.IntegrationBounds
import com.megakotl.core.TimedIntegral

@Composable
internal fun ControlsPanel(
    state: WorkspaceState,
    bounds: IntegrationBounds,
    result: TimedIntegral?,
    comparison: TimedIntegral?,
    error: String?,
    calculating: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(modifier, tonalElevation = 3.dp) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Настройки", style = MaterialTheme.typography.headlineSmall)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Построение функции", style = MaterialTheme.typography.titleMedium)
                    ModePicker(state.kind, CurveKind.entries, { it.title }) { state.kind = it }
                    if (state.kind == CurveKind.FORMULA) {
                        OutlinedTextField(
                            value = state.formula,
                            onValueChange = { state.formula = it },
                            label = { Text("f(x) — например sin(x)+x^2") },
                            singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        PointEditor(state)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SmallField("X от", state.xMin, { state.xMin = it }, Modifier.weight(1f))
                        SmallField("X до", state.xMax, { state.xMax = it }, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = {
                            state.viewport = state.viewport.copy(scale = (state.viewport.scale / 1.25).coerceAtLeast(8.0))
                        }, modifier = Modifier.weight(1f)) { Text("−") }
                        OutlinedButton(onClick = state::resetView, modifier = Modifier.weight(1f)) { Text("Центр") }
                        OutlinedButton(onClick = {
                            state.viewport = state.viewport.copy(scale = (state.viewport.scale * 1.25).coerceAtMost(5000.0))
                        }, modifier = Modifier.weight(1f)) { Text("+") }
                    }
                    OutlinedButton(onClick = { state.showNodes = !state.showNodes }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (state.showNodes) "Скрыть узлы" else "Показать узлы")
                    }
                }
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Вычисление интеграла", style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Интегрировать по всему X от…до", modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall)
                        Switch(checked = state.integrateFullRange,
                            onCheckedChange = { state.integrateFullRange = it })
                    }
                    if (state.integrateFullRange) {
                        if (state.kind == CurveKind.SPLINE && state.points.size >= 2) {
                            val start = state.points.minOf { it.x }
                            val end = state.points.maxOf { it.x }
                            Text("Сплайн определён между крайними узлами: " +
                                "[${shortNumber(start)}; ${shortNumber(end)}]. " +
                                "Интегрируется пересечение с диапазоном X от…до.",
                                style = MaterialTheme.typography.bodySmall)
                        } else {
                            Text("Интегрирование по диапазону построения: " +
                                "[${state.xMin}; ${state.xMax}].",
                                style = MaterialTheme.typography.bodySmall)
                        }
                        if (bounds.valid) {
                            Text("Фактические границы: " +
                                "[${shortNumber(requireNotNull(bounds.a))}; ${shortNumber(requireNotNull(bounds.b))}]",
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SmallField("a",
                            if (state.integrateFullRange) bounds.a?.let(::shortNumber) ?: "" else state.a,
                            { state.a = it }, Modifier.weight(1f), enabled = !state.integrateFullRange)
                        SmallField("b",
                            if (state.integrateFullRange) bounds.b?.let(::shortNumber) ?: "" else state.b,
                            { state.b = it }, Modifier.weight(1f), enabled = !state.integrateFullRange)
                        SmallField("n", state.n, { state.n = it }, Modifier.weight(1f))
                    }
                    Text("Число разбиений — быстрое изменение (4–400)", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = (state.n.toFloatOrNull() ?: 40f).coerceIn(4f, 400f),
                        onValueChange = { state.n = it.toInt().toString() },
                        valueRange = 4f..400f
                    )
                    Text("Метод численного интегрирования", style = MaterialTheme.typography.labelMedium)
                    ModePicker(state.method, IntegralMethod.entries, { it.title }) { state.method = it }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Показывать разбиения", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        Switch(checked = state.showArea, onCheckedChange = { state.showArea = it })
                    }
                    HorizontalDivider()
                    Text("Режим вычисления", style = MaterialTheme.typography.labelMedium)
                    ModePicker(state.execution, ExecutionMode.entries, { it.title }) { state.execution = it }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text("Сравнить оба режима", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        Switch(checked = state.compare, onCheckedChange = { state.compare = it })
                    }
                    Text("Корутины разбивают N интервалов между независимыми задачами в Dispatchers.Default.", style = MaterialTheme.typography.bodySmall)
                    if (state.n.toIntOrNull()?.let { it > 120 } == true) {
                        Text("При n > 120 заливка разбиений скрыта, вычисление производится полностью.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            ResultsPanel(state, bounds, result, comparison, error, calculating)
        }
    }
}

@Composable
private fun SmallField(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    enabled: Boolean = true
) {
    OutlinedTextField(value, onValueChange, modifier = modifier, enabled = enabled,
        label = { Text(title) }, singleLine = true)
}

@Composable
private fun <T> ModePicker(current: T, values: List<T>, title: (T) -> String, onSelect: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(title(current), modifier = Modifier.weight(1f))
            Text("  ▾")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            values.forEach { choice ->
                DropdownMenuItem(text = { Text(title(choice)) }, onClick = {
                    expanded = false
                    onSelect(choice)
                })
            }
        }
    }
}

@Composable
private fun PointEditor(state: WorkspaceState) {
    var x by remember { mutableStateOf("0") }
    var y by remember { mutableStateOf("0") }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        SmallField("x", x, { x = it }, Modifier.weight(1f))
        SmallField("y", y, { y = it }, Modifier.weight(1f))
        Button(onClick = {
            val px = parseDouble(x)
            val py = parseDouble(y)
            if (px != null && py != null) state.addPoint(GraphPoint(px, py))
            else state.status = "Введите две корректные координаты"
        }) { Text("+") }
    }
    Text("Точки (${state.points.size}/20). Нажимайте на график или вводите координаты.", style = MaterialTheme.typography.bodySmall)
    state.points.sortedBy { it.x }.forEach { point ->
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("(${shortNumber(point.x)}; ${shortNumber(point.y)})", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = { state.points.remove(point) }) { Text("×") }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedButton(onClick = { if (state.points.isNotEmpty()) state.points.removeAt(state.points.lastIndex) }, modifier = Modifier.weight(1f)) {
            Text("Отменить узел")
        }
        OutlinedButton(onClick = { state.points.clear() }, modifier = Modifier.weight(1f)) {
            Text("Очистить")
        }
    }
    OutlinedButton(onClick = {
        state.points.clear()
        state.points.addAll(listOf(GraphPoint(-3.0, -1.0), GraphPoint(-1.0, 1.0), GraphPoint(1.0, 0.0), GraphPoint(3.0, 2.0)))
        state.status = "Пример узлов загружен"
    }, modifier = Modifier.fillMaxWidth()) { Text("Пример интерполяции") }
}
