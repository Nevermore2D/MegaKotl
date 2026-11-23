package com.megakotl.features.polynomials

class LagrangePolynomial(vararg nodes: Point) : Polynomial() {

    private val xNodes = DoubleArray(nodes.size) { nodes[it].x }
    private val yNodes = DoubleArray(nodes.size) { nodes[it].y }

    override fun calculate(x: Double): Double {
        var result = 0.0

        for (i in xNodes.indices) {
            var basis = 1.0
            val xi = xNodes[i]

            for (j in xNodes.indices) {
                if (i != j) {
                    val denominator = xi - xNodes[j]

                    if (denominator == 0.0) {
                        return Double.NaN
                    }

                    basis *= (x - xNodes[j]) / denominator
                }
            }

            result += yNodes[i] * basis
        }

        return result
    }
}
