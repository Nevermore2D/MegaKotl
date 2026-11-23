package com.megakotl.workspace

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.megakotl.core.CurveKind
import com.megakotl.core.ExecutionMode
import com.megakotl.core.GraphPoint
import com.megakotl.core.IntegralMethod
import com.megakotl.core.Viewport
import com.megakotl.core.decimalLabel
import kotlin.math.abs

class WorkspaceState {
    var kind by mutableStateOf(CurveKind.NEWTON)
    var formula by mutableStateOf("sin(x) + x / 3")
    val points = mutableStateListOf(
        GraphPoint(-3.0, -1.0), GraphPoint(-1.0, 1.0),
        GraphPoint(1.0, 0.0), GraphPoint(3.0, 2.0)
    )
    var viewport by mutableStateOf(Viewport())
    var xMin by mutableStateOf("-5")
    var xMax by mutableStateOf("5")
    var a by mutableStateOf("-2")
    var b by mutableStateOf("2")
    var n by mutableStateOf("40")
    var integrateFullRange by mutableStateOf(false)
    var method by mutableStateOf(IntegralMethod.MIDPOINT)
    var execution by mutableStateOf(ExecutionMode.SEQUENTIAL)
    var compare by mutableStateOf(false)
    var showArea by mutableStateOf(true)
    var showNodes by mutableStateOf(true)
    var status by mutableStateOf("ЛКМ по графику — точка; перетаскивание — перемещение; колесо — масштаб")

    fun addPoint(point: GraphPoint) {
        when {
            !point.x.isFinite() || !point.y.isFinite() -> status = "Некорректные координаты"
            points.size >= 20 -> status = "Достигнут предел в 20 точек"
            points.any { abs(it.x - point.x) < 1e-9 } -> status = "Узел с таким X уже существует"
            else -> {
                points.add(point)
                status = "Узел (${shortNumber(point.x)}, ${shortNumber(point.y)}) добавлен"
            }
        }
    }

    fun resetView() {
        viewport = Viewport()
        status = "Масштаб сброшен"
    }
}

internal fun parseDouble(text: String) = text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
internal fun shortNumber(number: Double) = if (number.isFinite()) decimalLabel(number, 5) else "—"
