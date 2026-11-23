package com.megakotl.workspace

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.megakotl.core.GraphPoint
import com.megakotl.core.Viewport
import kotlin.math.abs

internal fun DrawScope.drawCurve(
    view: Viewport,
    evaluator: (Double) -> Double,
    graphMin: Double,
    graphMax: Double
) {
    val w = size.width.toDouble()
    val h = size.height.toDouble()
    val path = Path()
    var connected = false
    var previousY = 0.0
    for (i in 0..1500) {
        val x = graphMin + (graphMax - graphMin) * i / 1500.0
        val y = evaluator(x)
        val point = view.toScreen(GraphPoint(x, y), w, h)
        if (!y.isFinite() || abs(y) > 1e9 || !point.y.isFinite() || abs(point.y) > 30_000) {
            connected = false
            continue
        }
        // Avoid diagonal strokes across poles and large numerical jumps.
        val jump = connected && abs(point.y - previousY) > 1.5 * h
        if (!connected || jump) path.moveTo(point.x.toFloat(), point.y.toFloat())
        else path.lineTo(point.x.toFloat(), point.y.toFloat())
        connected = true
        previousY = point.y
    }
    drawPath(path, color = Color(0xFF1967D2), style = Stroke(width = 2.8f))
}

internal fun DrawScope.drawNodes(view: Viewport, nodes: List<GraphPoint>) {
    val w = size.width.toDouble()
    val h = size.height.toDouble()
    nodes.forEach { node ->
        val screen = view.toScreen(node, w, h)
        if (screen.x.isFinite() && screen.y.isFinite() &&
            screen.x in -1000.0..(w + 1000) && screen.y in -1000.0..(h + 1000)
        ) {
            val center = Offset(screen.x.toFloat(), screen.y.toFloat())
            drawCircle(Color.White, radius = 6.7f, center = center)
            drawCircle(Color(0xFFE63946), radius = 5.0f, center = center)
        }
    }
}
