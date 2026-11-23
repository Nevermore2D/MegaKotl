package com.megakotl.features.polynomials

class NewtonPolynomial(vararg nodes: Point) : Polynomial() {

    private val xNodes = DoubleArray(nodes.size) { nodes[it].x }
    private val coefficients = DoubleArray(nodes.size)

    init {
        for (i in nodes.indices) {
            coefficients[i] = nodes[i].y
        }

        for (j in 1 until xNodes.size) {
            for (i in xNodes.size - 1 downTo j) {
                val denominator = xNodes[i] - xNodes[i - j]

                coefficients[i] =
                    if (denominator == 0.0) {
                        Double.NaN
                    } else {
                        (coefficients[i] - coefficients[i - 1]) / denominator
                    }
            }
        }
    }

    override fun calculate(x: Double): Double {
        if (coefficients.isEmpty()) return 0.0

        var result = coefficients.last()

        for (i in coefficients.size - 2 downTo 0) {
            result = result * (x - xNodes[i]) + coefficients[i]
        }

        return result
    }
}
