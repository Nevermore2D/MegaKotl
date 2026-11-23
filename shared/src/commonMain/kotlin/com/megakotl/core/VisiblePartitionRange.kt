package com.megakotl.core

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/** Range of partition indices intersecting the visible horizontal slice of the canvas.
 * If cells are narrower than a screen pixel, the overlay is drawn using pixel-sized
 * samples instead of issuing tens of thousands of overlapping drawing commands.
 * This affects only drawing, never the number of intervals used for integration.
 */
data class VisiblePartitionRange(
    val start: Int,
    val endExclusive: Int,
    val minX: Double,
    val maxX: Double,
    val dense: Boolean
)

fun visiblePartitionRange(
    a: Double,
    b: Double,
    n: Int,
    visibleMinX: Double,
    visibleMaxX: Double,
    pixelsPerUnit: Double
): VisiblePartitionRange? {
    if (n !in 1..20_000 || !a.isFinite() || !b.isFinite() || a >= b ||
        !visibleMinX.isFinite() || !visibleMaxX.isFinite() ||
        !pixelsPerUnit.isFinite() || pixelsPerUnit <= 0.0
    ) return null
    val left = max(a, visibleMinX)
    val right = min(b, visibleMaxX)
    if (left >= right) return null

    val dx = (b - a) / n
    val first = floor((left - a) / dx).toInt().coerceIn(0, n - 1)
    val last = ceil((right - a) / dx).toInt().coerceIn(first + 1, n)
    return VisiblePartitionRange(first, last, left, right, dx * pixelsPerUnit < 1.5)
}
