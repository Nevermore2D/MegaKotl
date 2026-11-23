package com.megakotl.features.spline

class Spline(var points: List<Point>) {
    fun buildSpline(): List<SplineSegment> {
        val n = points.size - 1
        if (n < 1) return emptyList()
        
        val h = MutableList(n) { 0.0 }
        val c = MutableList(n + 1) { 0.0 }
        val alpha = MutableList(n) { 0.0 }
        val beta = MutableList(n) { 0.0 }
        
        for (i in 0 until n) {
            h[i] = points[i + 1].x - points[i].x
        }
        
        for (i in 1 until n) {
            val A = h[i - 1]
            val C = 2.0 * (h[i - 1] + h[i])
            val B = h[i]

            val dy_next = (points[i + 1].y - points[i].y) / h[i]
            val dy_prev = (points[i].y - points[i - 1].y) / h[i - 1]
            val F = 3.0 * (dy_next - dy_prev)

            val z = A * alpha[i - 1] + C
            alpha[i] = -B / z
            beta[i] = (F - A * beta[i - 1]) / z
        }
        
        c[n] = 0.0
        
        for (i in n - 1 downTo 0) {
            c[i] = alpha[i] * c[i + 1] + beta[i]
        }
        
        return MutableList(n) { i ->
            SplineSegment(
                start = points[i].x,
                a = points[i].y,
                c = c[i],
                b = (points[i + 1].y - points[i].y) / h[i] - h[i] * (c[i + 1] + 2.0 * c[i]) / 3.0,
                d = (c[i + 1] - c[i]) / (3.0 * h[i])
            )
        }
    }
}
