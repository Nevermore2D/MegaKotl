package com.megakotl.features.polynomials

import kotlin.math.abs

private const val RANK_EPSILON = 1e-10

/** Решает задачу МНК модифицированным методом Грама — Шмидта с повторной ортогонализацией. Возвращает null при вырождении. */
internal fun solveLeastSquares(
    matrix: Array<DoubleArray>,
    values: DoubleArray
): DoubleArray? {
    val rowCount = matrix.size

    if (rowCount == 0) return null

    val columnCount = matrix[0].size

    if (rowCount < columnCount) return null

    // Q хранится по столбцам.
    val q = Array(columnCount) { DoubleArray(rowCount) }

    // R — верхнетреугольная матрица.
    val r = Array(columnCount) {
        DoubleArray(columnCount)
    }

    var largestColumnNorm = 0.0

    for (column in 0 until columnCount) {
        var normSquared = 0.0

        for (row in 0 until rowCount) {
            val value = matrix[row][column]
            normSquared += value * value
        }

        largestColumnNorm = maxOf(
            largestColumnNorm,
            kotlin.math.sqrt(normSquared)
        )
    }

    if (largestColumnNorm == 0.0) return null

    for (column in 0 until columnCount) {
        val vector = DoubleArray(rowCount) { row ->
            matrix[row][column]
        }

        // Ортогонализуем столбец относительно предыдущих.
        for (previous in 0 until column) {
            var projection = 0.0

            for (row in 0 until rowCount) {
                projection += q[previous][row] * vector[row]
            }

            r[previous][column] = projection

            for (row in 0 until rowCount) {
                vector[row] -= projection * q[previous][row]
            }
        }

        // Повторная ортогонализация повышает устойчивость.
        for (previous in 0 until column) {
            var correction = 0.0

            for (row in 0 until rowCount) {
                correction += q[previous][row] * vector[row]
            }

            r[previous][column] += correction

            for (row in 0 until rowCount) {
                vector[row] -= correction * q[previous][row]
            }
        }

        var normSquared = 0.0

        for (value in vector) {
            normSquared += value * value
        }

        val norm = kotlin.math.sqrt(normSquared)

        if (
            !norm.isFinite() ||
            norm <= RANK_EPSILON * largestColumnNorm
        ) {
            return null
        }

        r[column][column] = norm

        for (row in 0 until rowCount) {
            q[column][row] = vector[row] / norm
        }
    }

    // Вычисляем Qᵀy.
    val projectedValues = DoubleArray(columnCount)

    for (column in 0 until columnCount) {
        var sum = 0.0

        for (row in 0 until rowCount) {
            sum += q[column][row] * values[row]
        }

        projectedValues[column] = sum
    }

    // Решаем R*c = Qᵀy обратным ходом.
    val result = DoubleArray(columnCount)

    for (i in columnCount - 1 downTo 0) {
        var sum = projectedValues[i]

        for (j in i + 1 until columnCount) {
            sum -= r[i][j] * result[j]
        }

        val diagonal = r[i][i]

        if (abs(diagonal) < RANK_EPSILON * largestColumnNorm) {
            return null
        }

        result[i] = sum / diagonal

        if (!result[i].isFinite()) {
            return null
        }
    }

    return result
}
