package com.megakotl.core

import org.mariuszgromada.math.mxparser.Argument
import org.mariuszgromada.math.mxparser.Expression

internal actual fun formulaSyntaxError(expression: String): String? {
    val variable = Argument("x")
    return if (Expression(expression, variable).checkSyntax()) null else "Некорректное выражение"
}

internal actual fun createFormulaEvaluator(expression: String): (Double) -> Double {
    val variable = Argument("x")
    val parsed = Expression(expression, variable)
    return { x ->
        if (!x.isFinite()) Double.NaN else {
            variable.argumentValue = x
            parsed.calculate()
        }
    }
}
