package com.megakotl.features.polynomials

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Тригонометрический полином с периодом 2π.
 *
 * T(x) = c0 + Σ [ak*cos(k*x) + bk*sin(k*x)]
 *
 * Коэффициенты подбираются методом наименьших квадратов.
 * Для решения задачи используется QR-разложение
 * с ортогонализацией столбцов.
 *
 * Число гармоник определяется количеством узлов.
 */
class TrigPolynomial(vararg nodes: Point) : Polynomial() {

    companion object {
        const val PERIOD: Double = 2.0 * PI

        // Ограничиваем число гармоник для устойчивости вычислений.
        private const val MAX_HARMONICS = 12
    }

    private val coefficients: DoubleArray
    val harmonics: Int

    init {
        val validNodes = nodes.filter {
            it.x.isFinite() && it.y.isFinite()
        }

        if (validNodes.isEmpty()) {
            harmonics = 0
            coefficients = doubleArrayOf(0.0)
        } else {
            /*
             * При m гармониках число коэффициентов равно 2m + 1.
             * Начинаем с максимального подходящего количества.
             * Если матрица вырождена, уменьшаем число гармоник.
             */
            var selectedHarmonics = minOf(
                (validNodes.size - 1) / 2,
                MAX_HARMONICS
            )

            var selectedCoefficients: DoubleArray? = null

            while (selectedHarmonics >= 0) {
                val matrix = buildTrigBasis(
                    nodes = validNodes,
                    harmonics = selectedHarmonics
                )

                val values = DoubleArray(validNodes.size) {
                    validNodes[it].y
                }

                val solution = solveLeastSquares(
                    matrix = matrix,
                    values = values
                )

                if (solution != null) {
                    selectedCoefficients = solution
                    break
                }

                selectedHarmonics--
            }

            harmonics = selectedHarmonics.coerceAtLeast(0)
            coefficients = selectedCoefficients
                ?: doubleArrayOf(validNodes.map { it.y }.average())
        }
    }

    override fun calculate(x: Double): Double {
        if (!x.isFinite()) return Double.NaN

        var result = coefficients[0]

        for (k in 1..harmonics) {
            val angle = k * x

            val cosineCoefficient = coefficients[2 * k - 1]
            val sineCoefficient = coefficients[2 * k]

            result += cosineCoefficient * cos(angle)
            result += sineCoefficient * sin(angle)
        }

        return result
    }
}
