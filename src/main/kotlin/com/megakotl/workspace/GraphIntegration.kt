package com.megakotl.workspace

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.megakotl.core.GraphPoint
import com.megakotl.core.IntegralMethod
import com.megakotl.core.Viewport
import kotlin.math.max
import kotlin.math.min

/** The overlay always uses actual integration limits, NOT plot display limits. */
internal fun DrawScope.drawIntegralArea(
    view: Viewport,
    f: (Double) -> Double,
    a: Double,
    b: Double,
    n: Int,
    method: IntegralMethod
) {
    if (a >= b || n !in 1..120 || !a.isFinite() || !b.isFinite()) return
    val w = size.width.toDouble()
    val h = size.height.toDouble()
    val dx = (b - a) / n
    val zero = view.toScreen(GraphPoint(0.0, 0.0), w, h).y
    val isRect = method in listOf(
        IntegralMethod.LEFT, IntegralMethod.MIDPOINT, IntegralMethod.RIGHT,
        IntegralMethod.DARBOUX_LOWER, IntegralMethod.DARBOUX_UPPER
    )
    for (i in 0 until n) {
        val left = a + dx * i
        val right = if (i == n - 1) b else a + dx * (i + 1)
        val middle = (left + right) / 2
        val xs = if (isRect) listOf(
            when (method) {
                IntegralMethod.LEFT -> left
                IntegralMethod.RIGHT -> right
                IntegralMethod.MIDPOINT -> middle
                IntegralMethod.DARBOUX_LOWER, IntegralMethod.DARBOUX_UPPER -> {
                    val candidates = (0..32).map { j -> f(left + (right - left) * j / 32) }
                    if (method == IntegralMethod.DARBOUX_LOWER) candidates.minOrNull() ?: Double.NaN
                    else candidates.maxOrNull() ?: Double.NaN
                }
                else -> middle
            }
        ) else listOf(f(left), f(right))
        // Darboux's sampled bounds are y values. The remaining rectangle modes sample x.
        val values = if (isRect) listOf(
            if (method == IntegralMethod.DARBOUX_LOWER || method == IntegralMethod.DARBOUX_UPPER) xs[0]
            else f(xs[0])
        ) else xs
        if (values.any { !it.isFinite() || kotlin.math.abs(it) > 1e8 } || !zero.isFinite()) continue
        val start = view.toScreen(GraphPoint(left, 0.0), w, h)
        val end = view.toScreen(GraphPoint(right, 0.0), w, h)
        if (!start.x.isFinite() || !end.x.isFinite()) continue
        val good = values.average() >= 0
        val fill = if (good) Color(0x3535A86B) else Color(0x36E63946)
        val edge = if (good) Color(0xAA25945C) else Color(0xAAE63946)
        if (isRect) {
            val y = view.toScreen(GraphPoint(middle, values[0]), w, h).y
            if (!y.isFinite() || kotlin.math.abs(y) > 30_000 || kotlin.math.abs(zero) > 30_000) continue
            val p1 = Offset(min(start.x, end.x).toFloat(), min(y, zero).toFloat())
            val rect = Size(kotlin.math.abs(end.x - start.x).toFloat(), kotlin.math.abs(y - zero).toFloat())
            drawRect(fill, p1, rect)
            drawRect(edge, p1, rect, style = Stroke(width = 1f))
        } else {
            val polygon = Path()
            polygon.moveTo(start.x.toFloat(), zero.toFloat())
            polygon.lineTo(start.x.toFloat(), view.toScreen(GraphPoint(left, values[0]), w, h).y.toFloat())
            polygon.lineTo(end.x.toFloat(), view.toScreen(GraphPoint(right, values[1]), w, h).y.toFloat())
            polygon.lineTo(end.x.toFloat(), zero.toFloat())
            polygon.close()
            drawPath(polygon, fill)
            drawPath(polygon, edge, style = Stroke(width = 1f))
        }
    }
}
