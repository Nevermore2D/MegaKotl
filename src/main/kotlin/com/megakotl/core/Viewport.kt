package com.megakotl.core

import kotlin.math.exp

/** No UI types here: reusable for the drawing canvas and unit tests. */
data class Viewport(val centerX: Double = 0.0, val centerY: Double = 0.0, val scale: Double = 48.0) {
    fun toScreen(point: GraphPoint, width: Double, height: Double) = GraphPoint(
        width / 2 + (point.x - centerX) * scale,
        height / 2 - (point.y - centerY) * scale
    )
    fun toMath(px: Double, py: Double, width: Double, height: Double) = GraphPoint(
        centerX + (px - width / 2) / scale,
        centerY - (py - height / 2) / scale
    )
    fun drag(dx: Double, dy: Double) = copy(centerX = centerX - dx / scale, centerY = centerY + dy / scale)
    fun zoomAt(delta: Double, px: Double, py: Double, w: Double, h: Double): Viewport {
        val before = toMath(px, py, w, h)
        val newScale = (scale * exp(-delta * 0.12)).coerceIn(8.0, 5000.0)
        val next = copy(scale = newScale)
        val after = next.toMath(px, py, w, h)
        return next.copy(centerX = next.centerX + before.x - after.x, centerY = next.centerY + before.y - after.y)
    }
}
