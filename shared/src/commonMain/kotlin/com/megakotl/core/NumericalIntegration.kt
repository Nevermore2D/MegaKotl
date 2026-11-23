package com.megakotl.core

import kotlin.math.max
import kotlin.math.min

enum class IntegralMethod(val title: String) {
    LEFT("Левые прямоугольники"),
    MIDPOINT("Средние прямоугольники"),
    RIGHT("Правые прямоугольники"),
    TRAPEZOID("Трапеции"),
    DARBOUX_LOWER("Нижняя сумма Дарбу"),
    DARBOUX_UPPER("Верхняя сумма Дарбу")
}

data class IntegralValues(val value: Double, val lower: Double? = null, val upper: Double? = null)
internal data class PartialSums(val value: Double, val lower: Double, val upper: Double) {
    operator fun plus(other: PartialSums) = PartialSums(
        value + other.value, lower + other.lower, upper + other.upper
    )
}

/**
 * On each subinterval Darboux extrema are APPROXIMATED by 33 samples.
 * They are not certified analytical infima/suprema for arbitrary functions.
 */
internal fun integratePartition(
    f: (Double) -> Double,
    a: Double,
    b: Double,
    n: Int,
    method: IntegralMethod,
    begin: Int = 0,
    end: Int = n,
    checkCancellation: () -> Unit = {}
): PartialSums {
    require(n in 1..20_000 && a.isFinite() && b.isFinite() && a < b)
    val dx = (b - a) / n
    var total = 0.0
    var lower = 0.0
    var upper = 0.0

    fun evaluate(x: Double): Double {
        val y = f(x)
        require(y.isFinite()) { "Функция не определена на интервале интегрирования" }
        return y
    }

    for (i in begin until end) {
        if ((i - begin) % 32 == 0) checkCancellation()
        val x0 = a + dx * i
        val x1 = if (i == n - 1) b else a + dx * (i + 1)
        val xm = (x0 + x1) / 2.0
        val area = when (method) {
            IntegralMethod.LEFT -> evaluate(x0) * dx
            IntegralMethod.MIDPOINT -> evaluate(xm) * dx
            IntegralMethod.RIGHT -> evaluate(x1) * dx
            IntegralMethod.TRAPEZOID -> (evaluate(x0) + evaluate(x1)) * dx / 2.0
            IntegralMethod.DARBOUX_LOWER, IntegralMethod.DARBOUX_UPPER -> {
                var lo = Double.POSITIVE_INFINITY
                var hi = Double.NEGATIVE_INFINITY
                for (j in 0..32) {
                    val y = evaluate(x0 + (x1 - x0) * j / 32.0)
                    lo = min(lo, y)
                    hi = max(hi, y)
                }
                lower += lo * dx
                upper += hi * dx
                (if (method == IntegralMethod.DARBOUX_LOWER) lo else hi) * dx
            }
        }
        total += area
        require(total.isFinite()) { "Переполнение результата" }
    }
    return PartialSums(total, lower, upper)
}

fun integrateSequential(
    f: (Double) -> Double, a: Double, b: Double, n: Int, method: IntegralMethod
): IntegralValues {
    val p = integratePartition(f, a, b, n, method)
    return resultOf(p, method)
}

internal fun resultOf(partial: PartialSums, method: IntegralMethod): IntegralValues =
    if (method == IntegralMethod.DARBOUX_LOWER || method == IntegralMethod.DARBOUX_UPPER) {
        IntegralValues(partial.value, partial.lower, partial.upper)
    } else IntegralValues(partial.value)
