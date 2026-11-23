package com.megakotl.workspace

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.sp
import com.megakotl.core.GraphPoint
import com.megakotl.core.Viewport
import com.megakotl.core.axisTickLabel
import com.megakotl.core.axisTickStep
import kotlin.math.ceil

/** Underlay: adaptive grid and the genuine X/Y axes. */
internal fun DrawScope.drawGrid(view: Viewport) {
    val w = size.width.toDouble()
    val h = size.height.toDouble()
    if (w <= 0 || h <= 0) return
    val step = axisTickStep(view.scale)
    val x0 = view.centerX - w / (2 * view.scale)
    val x1 = view.centerX + w / (2 * view.scale)
    val y0 = view.centerY - h / (2 * view.scale)
    val y1 = view.centerY + h / (2 * view.scale)
    val gridColor = Color(0xFFE1E5EC)
    val axisColor = Color(0xFF68768A)

    var x = ceil(x0 / step) * step
    var count = 0
    while (x <= x1 && count++ < 300) {
        val px = view.toScreen(GraphPoint(x, 0.0), w, h).x.toFloat()
        drawLine(gridColor, Offset(px, 0f), Offset(px, size.height), 1f)
        x += step
    }
    var y = ceil(y0 / step) * step
    count = 0
    while (y <= y1 && count++ < 300) {
        val py = view.toScreen(GraphPoint(0.0, y), w, h).y.toFloat()
        drawLine(gridColor, Offset(0f, py), Offset(size.width, py), 1f)
        y += step
    }
    val origin = view.toScreen(GraphPoint(0.0, 0.0), w, h)
    if (origin.x in 0.0..w) {
        drawLine(axisColor, Offset(origin.x.toFloat(), 0f),
            Offset(origin.x.toFloat(), size.height), 2f)
    }
    if (origin.y in 0.0..h) {
        drawLine(axisColor, Offset(0f, origin.y.toFloat()),
            Offset(size.width, origin.y.toFloat()), 2f)
    }
}

/** Overlay: numbered ruler ticks, drawn after the curve and integral fill for readability. */
internal fun DrawScope.drawAxisRulers(view: Viewport, textMeasurer: TextMeasurer) {
    val w = size.width.toDouble()
    val h = size.height.toDouble()
    if (w < 80 || h < 50) return
    val step = axisTickStep(view.scale)
    val x0 = view.centerX - w / (2 * view.scale)
    val x1 = view.centerX + w / (2 * view.scale)
    val y0 = view.centerY - h / (2 * view.scale)
    val y1 = view.centerY + h / (2 * view.scale)
    val axisColor = Color(0xFF68768A)
    val textStyle = TextStyle(color = Color(0xFF35445A), fontSize = 11.sp)
    val origin = view.toScreen(GraphPoint(0.0, 0.0), w, h)

    // If an axis is offscreen, pin its numerical ruler to the nearest canvas edge.
    val rulerY = origin.y.toFloat().coerceIn(8f, size.height - 23f)
    val rulerX = origin.x.toFloat().coerceIn(54f, size.width - 8f)
    val tickLength = 4f

    var x = ceil(x0 / step) * step
    var count = 0
    while (x <= x1 && count++ < 300) {
        val px = view.toScreen(GraphPoint(x, 0.0), w, h).x.toFloat()
        if (px in 0f..size.width) {
            val label = axisTickLabel(x, step)
            // The Y ruler handles the common zero at a visible origin.
            if (label != "0" || origin.x !in 0.0..w || origin.y !in 0.0..h) {
                val layout = textMeasurer.measure(label, style = textStyle)
                val left = (px - layout.size.width / 2f).coerceIn(2f,
                    (size.width - layout.size.width - 2f).coerceAtLeast(2f))
                drawLine(axisColor, Offset(px, rulerY - tickLength), Offset(px, rulerY + tickLength), 1.5f)
                drawText(layout, topLeft = Offset(left, rulerY + 5f))
            }
        }
        x += step
    }

    var y = ceil(y0 / step) * step
    count = 0
    while (y <= y1 && count++ < 300) {
        val py = view.toScreen(GraphPoint(0.0, y), w, h).y.toFloat()
        if (py in 0f..size.height) {
            val label = axisTickLabel(y, step)
            val layout = textMeasurer.measure(label, style = textStyle)
            val left = (rulerX - layout.size.width - 8f).coerceAtLeast(2f)
            val top = (py - layout.size.height / 2f).coerceIn(1f,
                (size.height - layout.size.height - 1f).coerceAtLeast(1f))
            drawLine(axisColor, Offset(rulerX - tickLength, py), Offset(rulerX + tickLength, py), 1.5f)
            drawText(layout, topLeft = Offset(left, top))
        }
        y += step
    }
}
