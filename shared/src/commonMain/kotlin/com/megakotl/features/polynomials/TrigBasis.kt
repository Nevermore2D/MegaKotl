package com.megakotl.features.polynomials

import kotlin.math.cos
import kotlin.math.sin

/** Строит базис [1, cos(x), sin(x), cos(2x), sin(2x), ...]. */
internal fun buildTrigBasis(
    nodes: List<Point>,
    harmonics: Int
): Array<DoubleArray> {
    val columnCount = 2 * harmonics + 1

    return Array(nodes.size) { row ->
        val x = nodes[row].x
        val values = DoubleArray(columnCount)

        values[0] = 1.0

        for (k in 1..harmonics) {
            values[2 * k - 1] = cos(k * x)
            values[2 * k] = sin(k * x)
        }

        values
    }
}
