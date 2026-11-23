package com.megakotl.core

import com.megakotl.features.polynomials.LagrangePolynomial
import com.megakotl.features.polynomials.NewtonPolynomial
import com.megakotl.features.polynomials.Point as PolynomialPoint
import com.megakotl.features.polynomials.TrigPolynomial
import com.megakotl.features.spline.Point as SplinePoint
import com.megakotl.features.spline.Spline
import org.mariuszgromada.math.mxparser.Argument
import org.mariuszgromada.math.mxparser.Expression
import kotlin.math.abs

/** A single mathematical curve is shared by the plot and all numerical methods. */
data class GraphPoint(val x: Double, val y: Double)

enum class CurveKind(val title: String) {
    FORMULA("Формула f(x)"),
    LAGRANGE("Полином Лагранжа"),
    NEWTON("Полином Ньютона"),
    TRIGONOMETRIC("Тригонометрический МНК"),
    SPLINE("Кубический сплайн")
}

/** Immutable snapshot. Each evaluator call creates its own expression state for thread safety. */
data class CurveDefinition(
    val kind: CurveKind,
    val points: List<GraphPoint>,
    val formula: String
) {
    fun validationError(): String? {
        if (kind == CurveKind.FORMULA) {
            if (formula.isBlank()) return "Введите формулу f(x)"
            val argument = Argument("x")
            if (!Expression(formula, argument).checkSyntax()) return "Некорректная формула f(x)"
            return null
        }
        if (points.size < 2) return "Нужно минимум две точки (щёлкните на графике)"
        if (points.size > 20) return "Максимум 20 узлов"
        if (points.any { !it.x.isFinite() || !it.y.isFinite() }) return "Координаты должны быть конечными"
        val sorted = points.sortedBy { it.x }
        if (sorted.zipWithNext().any { (a, b) -> abs(a.x - b.x) < 1e-9 }) {
            return "Координаты X не должны повторяться"
        }
        return null
    }

    fun newEvaluator(): ((Double) -> Double)? {
        if (validationError() != null) return null
        return when (kind) {
            CurveKind.FORMULA -> {
                val variable = Argument("x")
                val expression = Expression(formula, variable)
                val f: (Double) -> Double = { x ->
                    if (!x.isFinite()) Double.NaN
                    else {
                        variable.argumentValue = x
                        expression.calculate()
                    }
                }
                f
            }
            CurveKind.LAGRANGE, CurveKind.NEWTON, CurveKind.TRIGONOMETRIC -> {
                val nodes = points.sortedBy { it.x }.map { PolynomialPoint(it.x, it.y) }.toTypedArray()
                val polynomial = when (kind) {
                    CurveKind.LAGRANGE -> LagrangePolynomial(*nodes)
                    CurveKind.NEWTON -> NewtonPolynomial(*nodes)
                    else -> TrigPolynomial(*nodes)
                }
                val f: (Double) -> Double = { x -> polynomial.calculate(x) }
                f
            }
            CurveKind.SPLINE -> {
                val sorted = points.sortedBy { it.x }
                val segments = Spline(sorted.map { SplinePoint(it.x, it.y) }).buildSpline()
                val lastX = sorted.last().x
                val f: (Double) -> Double = { x ->
                    if (x < sorted.first().x || x > lastX || !x.isFinite()) Double.NaN
                    else {
                        val index = segments.indices.lastOrNull { segments[it].start <= x } ?: 0
                        val segment = segments[index]
                        val dx = x - segment.start
                        segment.a + dx * (segment.b + dx * (segment.c + dx * segment.d))
                    }
                }
                f
            }
        }
    }

    /** Polynomials/formulae have no finite integration domain restriction here. */
    fun integrationDomain(): IntegrationBounds? =
        if (kind == CurveKind.SPLINE && points.size >= 2) {
            IntegrationBounds(points.minOf { it.x }, points.maxOf { it.x })
        } else null

    fun domainError(a: Double, b: Double): String? =
        if (integrationDomain()?.let { a < requireNotNull(it.a) || b > requireNotNull(it.b) } == true)
            "Для сплайна [a, b] должен лежать между крайними узлами"
        else null
}
