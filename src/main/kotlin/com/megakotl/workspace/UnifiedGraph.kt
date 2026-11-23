package com.megakotl.workspace

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.input.pointer.pointerInput
import com.megakotl.core.CurveKind
import kotlin.math.max
import kotlin.math.min

@Composable
internal fun UnifiedGraph(
    state: WorkspaceState,
    evaluator: ((Double) -> Double)?,
    plotMin: Double?,
    plotMax: Double?,
    a: Double?,
    b: Double?,
    n: Int?
) {
    val textMeasurer = rememberTextMeasurer()
    Canvas(
        Modifier.fillMaxSize()
            .clipToBounds()
            .background(Color.White)
            .pointerInput(state) {
                detectTapGestures { offset ->
                    val point = state.viewport.toMath(
                        offset.x.toDouble(), offset.y.toDouble(),
                        size.width.toDouble(), size.height.toDouble()
                    )
                    state.addPoint(point)
                    if (state.kind == CurveKind.FORMULA) {
                        state.status = "Точка сохранена; переключитесь на метод интерполяции, чтобы использовать узлы"
                    }
                }
            }
            .pointerInput(state) {
                detectDragGestures { change, delta ->
                    state.viewport = state.viewport.drag(delta.x.toDouble(), delta.y.toDouble())
                    change.consume()
                }
            }
            .pointerInput(state) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Scroll) {
                            val change = event.changes.firstOrNull() ?: continue
                            state.viewport = state.viewport.zoomAt(
                                change.scrollDelta.y.toDouble(),
                                change.position.x.toDouble(), change.position.y.toDouble(),
                                size.width.toDouble(), size.height.toDouble()
                            )
                            change.consume()
                        }
                    }
                }
            }
    ) {
        drawGrid(state.viewport)
        if (evaluator != null) {
            val visualStart = state.viewport.centerX - size.width.toDouble() / (2 * state.viewport.scale)
            val visualEnd = state.viewport.centerX + size.width.toDouble() / (2 * state.viewport.scale)
            if (state.showArea && a != null && b != null && a < b && n != null &&
                (state.kind != CurveKind.SPLINE || (state.points.size >= 2 && a >= state.points.minOf { it.x } && b <= state.points.maxOf { it.x }))
            ) {
                drawIntegralArea(state.viewport, evaluator, a, b, n, state.method)
            }
            val minX = if (plotMin != null && plotMax != null && plotMin < plotMax) max(plotMin, visualStart) else visualStart
            val maxX = if (plotMin != null && plotMax != null && plotMin < plotMax) min(plotMax, visualEnd) else visualEnd
            if (minX < maxX) drawCurve(state.viewport, evaluator, minX, maxX)
        }
        if (state.showNodes && state.kind != CurveKind.FORMULA) {
            drawNodes(state.viewport, state.points)
        }
        drawAxisRulers(state.viewport, textMeasurer)
    }
}
