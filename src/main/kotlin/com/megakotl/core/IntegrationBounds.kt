package com.megakotl.core

/** Shared integration boundaries: calculations, graph overlays and result labels use the same pair. */
data class IntegrationBounds(val a: Double?, val b: Double?) {
    val valid: Boolean get() = a != null && b != null && a.isFinite() && b.isFinite() && a < b
}

/**
 * When full graph integration is enabled, only the part on which a curve is defined
 * can be integrated. A spline is defined between its extreme knots; unlike a
 * polynomial, it must not be extrapolated beyond them.
 *
 * In manual mode the entered a/b are preserved, so the normal domain validation
 * can reject an out-of-range interval instead of silently changing user input.
 */
fun resolveIntegrationBounds(
    fullGraphRange: Boolean,
    manualA: Double?,
    manualB: Double?,
    xMin: Double?,
    xMax: Double?,
    definedInterval: IntegrationBounds? = null
): IntegrationBounds {
    if (!fullGraphRange) return IntegrationBounds(manualA, manualB)
    val graphBounds = IntegrationBounds(xMin, xMax)
    if (!graphBounds.valid || definedInterval == null || !definedInterval.valid) return graphBounds
    return IntegrationBounds(
        maxOf(requireNotNull(xMin), requireNotNull(definedInterval.a)),
        minOf(requireNotNull(xMax), requireNotNull(definedInterval.b))
    )
}
