package com.megakotl.workspace

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.megakotl.core.IntegralMethod
import com.megakotl.core.Viewport
import com.megakotl.core.visiblePartitionRange
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

private val positiveArea = Color(0x3535A86B)
private val negativeArea = Color(0x36E63946)
private val positiveEdge = Color(0xAA25945C)
private val negativeEdge = Color(0xAAE63946)

/** Draw actual partitions if their boundaries are visible at current zoom.
 * When they become subpixel, draw a representative filled slice for each pixel.
 * In both cases integration itself still uses exactly n partitions.
 */
internal fun DrawScope.drawIntegralArea(
    view: Viewport,
    f: (Double) -> Double,
    a: Double,
    b: Double,
    n: Int,
    method: IntegralMethod
) {
    val width = size.width.toDouble()
    val height = size.height.toDouble()
    if (width <= 0.0 || height <= 0.0) return

    val visibleMin = view.centerX - width / (2.0 * view.scale)
    val visibleMax = view.centerX + width / (2.0 * view.scale)
    val range = visiblePartitionRange(a, b, n, visibleMin, visibleMax, view.scale) ?: return
    val dx = (b - a) / n
    val baseline = (height / 2.0 + view.centerY * view.scale).toFloat()
    if (!baseline.isFinite()) return
    val safeBaseline = baseline.coerceIn(-size.height * 2, size.height * 3)
    fun screenX(x: Double): Float = (width / 2.0 + (x - view.centerX) * view.scale).toFloat()
    fun screenY(y: Double): Float? {
        if (!y.isFinite() || abs(y) > 1e8) return null
        val yy = (height / 2.0 - (y - view.centerY) * view.scale).toFloat()
        return if (yy.isFinite()) yy.coerceIn(-size.height * 2, size.height * 3) else null
    }

    /** Height consistent with the chosen quadrature rule, including sampled Darboux extrema. */
    fun cellHeight(index: Int, atX: Double): Double? {
        val left = a + index * dx
        val right = if (index == n - 1) b else a + (index + 1) * dx
        val value = when (method) {
            IntegralMethod.LEFT -> f(left)
            IntegralMethod.MIDPOINT -> f((left + right) / 2.0)
            IntegralMethod.RIGHT -> f(right)
            IntegralMethod.TRAPEZOID -> {
                val t = ((atX - left) / (right - left)).coerceIn(0.0, 1.0)
                f(left) * (1.0 - t) + f(right) * t
            }
            IntegralMethod.DARBOUX_LOWER, IntegralMethod.DARBOUX_UPPER -> {
                var low = Double.POSITIVE_INFINITY
                var high = Double.NEGATIVE_INFINITY
                for (j in 0..32) {
                    val y = f(left + (right - left) * j / 32.0)
                    if (!y.isFinite()) return null
                    low = min(low, y)
                    high = max(high, y)
                }
                if (method == IntegralMethod.DARBOUX_LOWER) low else high
            }
        }
        return value.takeIf { it.isFinite() }
    }

    if (range.dense) {
        // The width of one partition is below 1.5 px, so drawing its outline
        // would obscure the graph. Draw the signed area at screen resolution.
        // As the user zooms in, individual partitions become visible automatically.
        val leftPx = max(0f, screenX(range.minX))
        val rightPx = min(size.width, screenX(range.maxX))
        var px = leftPx
        val stepPx = max(1f, (rightPx - leftPx) / 1500f)
        while (px < rightPx) {
            val nextPx = min(rightPx, px + stepPx)
            val sampleX = view.centerX + ((px + nextPx) / 2.0 - width / 2.0) / view.scale
            val index = floor((sampleX - a) / dx).toInt().coerceIn(0, n - 1)
            val y = cellHeight(index, sampleX)?.let(::screenY)
            if (y != null) {
                drawRect(
                    color = if (y <= safeBaseline) positiveArea else negativeArea,
                    topLeft = Offset(px, min(y, safeBaseline)),
                    size = Size(nextPx - px, abs(y - safeBaseline))
                )
            }
            px = nextPx
        }
    } else {
        // Only iterate over the partitions that intersect the canvas.
        for (index in range.start until range.endExclusive) {
            val left = a + index * dx
            val right = if (index == n - 1) b else a + (index + 1) * dx
            val x0 = screenX(left)
            val x1 = screenX(right)
            if (!x0.isFinite() || !x1.isFinite()) continue
            val sample = (left + right) / 2.0
            val y = cellHeight(index, sample)?.let(::screenY) ?: continue
            val positive = y <= safeBaseline
            val fill = if (positive) positiveArea else negativeArea
            val edge = if (positive) positiveEdge else negativeEdge
            if (method == IntegralMethod.TRAPEZOID) {
                val y0 = screenY(f(left)) ?: continue
                val y1 = screenY(f(right)) ?: continue
                val polygon = Path().apply {
                    moveTo(x0, safeBaseline)
                    lineTo(x0, y0)
                    lineTo(x1, y1)
                    lineTo(x1, safeBaseline)
                    close()
                }
                drawPath(polygon, fill)
                if (x1 - x0 >= 3f) drawPath(polygon, edge, style = Stroke(width = 1f))
            } else {
                val top = min(y, safeBaseline)
                val rectWidth = abs(x1 - x0)
                val rectHeight = abs(y - safeBaseline)
                drawRect(fill, Offset(min(x0, x1), top), Size(rectWidth, rectHeight))
                if (rectWidth >= 3f) {
                    drawRect(edge, Offset(min(x0, x1), top), Size(rectWidth, rectHeight), style = Stroke(width = 1f))
                }
            }
        }
    }
}
