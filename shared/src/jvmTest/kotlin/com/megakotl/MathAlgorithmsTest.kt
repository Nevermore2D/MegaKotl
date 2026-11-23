package com.megakotl

import com.megakotl.core.CurveDefinition
import com.megakotl.core.CurveKind
import com.megakotl.core.ExecutionMode
import com.megakotl.core.GraphPoint
import com.megakotl.core.IntegralMethod
import com.megakotl.core.IntegrationBounds
import com.megakotl.core.integrateSequential
import com.megakotl.core.resolveIntegrationBounds
import com.megakotl.core.axisTickLabel
import com.megakotl.core.axisTickStep
import com.megakotl.core.visiblePartitionRange
import com.megakotl.core.runIntegral
import com.megakotl.features.polynomials.LagrangePolynomial
import com.megakotl.features.polynomials.NewtonPolynomial
import com.megakotl.features.polynomials.Point
import com.megakotl.features.polynomials.TrigPolynomial
import com.megakotl.features.spline.Point as SplinePoint
import com.megakotl.features.spline.Spline
import kotlinx.coroutines.runBlocking
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MathAlgorithmsTest {
    @Test fun lagrangeAndNewtonInterpolateNodes() {
        val nodes = arrayOf(Point(0.0, 1.0), Point(1.0, 3.0))
        assertEquals(2.0, LagrangePolynomial(*nodes).calculate(0.5), 1e-10)
        assertEquals(2.0, NewtonPolynomial(*nodes).calculate(0.5), 1e-10)
    }

    @Test fun trigPolynomialOfConstantValues() {
        val p = TrigPolynomial(Point(0.0, 3.0), Point(1.0, 3.0), Point(2.0, 3.0))
        assertTrue(abs(p.calculate(1.5) - 3.0) < 1e-8)
    }

    @Test fun quadratureMethodsForLinearFunction() {
        val f: (Double) -> Double = { it }
        for (method in listOf(IntegralMethod.MIDPOINT, IntegralMethod.TRAPEZOID)) {
            assertEquals(2.0, integrateSequential(f, 0.0, 2.0, 40, method).value, 1e-10)
        }
    }

    @Test fun trapezoidIntegratesLinearExactly() {
        val f: (Double) -> Double = { 2 * it + 1 }
        assertEquals(6.0, integrateSequential(f, 0.0, 2.0, 9, IntegralMethod.TRAPEZOID).value, 1e-12)
    }

    @Test fun tickLabelsTrackZoomAndHandleNegativeValues() {
        assertEquals(2.0, axisTickStep(48.0), 1e-10)
        assertEquals(0.05, axisTickStep(2500.0), 1e-10)
        assertEquals("-1.25", axisTickLabel(-1.25, 0.05))
        assertEquals("0", axisTickLabel(-0.0, 0.05))
    }

    @Test fun graphRangeToggleChangesWhichBoundsAreUsed() {
        val manual = resolveIntegrationBounds(false, -2.0, 2.0, -5.0, 5.0)
        val full = resolveIntegrationBounds(true, -2.0, 2.0, -5.0, 5.0)
        assertEquals(-2.0, manual.a)
        assertEquals(2.0, manual.b)
        assertEquals(-5.0, full.a)
        assertEquals(5.0, full.b)
        assertTrue(full.valid)
        assertTrue(!resolveIntegrationBounds(true, -2.0, 2.0, 3.0, -1.0).valid)
        assertTrue(!resolveIntegrationBounds(true, -2.0, 2.0, null, 5.0).valid)
    }

    @Test fun splineFullRangeIsClippedToItsKnotDomain() {
        val curve = CurveDefinition(CurveKind.SPLINE,
            listOf(GraphPoint(-3.0, -1.0), GraphPoint(-1.0, 1.0),
                GraphPoint(1.0, 0.0), GraphPoint(3.0, 2.0)), "")
        val domain = assertNotNull(curve.integrationDomain())
        val full = resolveIntegrationBounds(true, -2.0, 2.0, -5.0, 5.0, domain)
        assertEquals(-3.0, full.a)
        assertEquals(3.0, full.b)
        assertTrue(full.valid)
        val f = assertNotNull(curve.newEvaluator())
        val integral = integrateSequential(f, requireNotNull(full.a), requireNotNull(full.b),
            40, IntegralMethod.TRAPEZOID)
        assertTrue(integral.value.isFinite())
    }

    @Test fun splineFullRangeUsesOverlapAndRejectsDisjointPlots() {
        val domain = IntegrationBounds(-3.0, 3.0)
        val overlap = resolveIntegrationBounds(true, -2.0, 2.0, 1.0, 5.0, domain)
        assertEquals(1.0, overlap.a)
        assertEquals(3.0, overlap.b)
        assertTrue(overlap.valid)
        val disjoint = resolveIntegrationBounds(true, -2.0, 2.0, 4.0, 5.0, domain)
        assertTrue(!disjoint.valid)
        val manual = resolveIntegrationBounds(false, -4.0, 1.0, -5.0, 5.0, domain)
        assertEquals(-4.0, manual.a) // manual bounds are never silently clamped
    }

    @Test fun sampledDarbouxBoundsEncloseIntegralOfMonotoneFunction() {
        val f: (Double) -> Double = { it * it }
        val low = integrateSequential(f, 0.0, 1.0, 10, IntegralMethod.DARBOUX_LOWER)
        val high = integrateSequential(f, 0.0, 1.0, 10, IntegralMethod.DARBOUX_UPPER)
        assertTrue(low.value <= 1.0 / 3.0)
        assertTrue(high.value >= 1.0 / 3.0)
        assertEquals(low.lower, high.lower)
        assertEquals(low.upper, high.upper)
    }

    @Test fun splineForTwoNodesIsLine() {
        val segments = Spline(listOf(SplinePoint(0.0, 0.0), SplinePoint(1.0, 2.0))).buildSpline()
        assertEquals(1, segments.size)
        assertEquals(2.0, segments.first().b, 1e-10)
    }

    @Test fun splineRequiresIntegrationWithinNodes() {
        val curve = CurveDefinition(CurveKind.SPLINE, listOf(GraphPoint(0.0, 0.0), GraphPoint(2.0, 4.0)), "")
        assertNull(curve.validationError())
        assertEquals("Для сплайна [a, b] должен лежать между крайними узлами", curve.domainError(-1.0, 1.0))
        assertEquals(2.0, assertNotNull(curve.newEvaluator()).invoke(1.0), 1e-10)
    }

    @Test fun exactNumberOfPartitionsIsUsedAndReported() = runBlocking {
        val curve = CurveDefinition(CurveKind.NEWTON,
            listOf(GraphPoint(0.0, 0.0), GraphPoint(1.0, 1.0), GraphPoint(2.0, 4.0)), "")
        for (n in listOf(1, 125, 400, 2000, 20_000)) {
            val result = runIntegral(curve, 0.0, 2.0, n, IntegralMethod.LEFT, ExecutionMode.SEQUENTIAL)
            assertEquals(n, result.partitions)
            // Exact left-Riemann formula for x^2 on [0,2].
            val expected = (8.0 / 3.0) - (4.0 / n) + (4.0 / (3.0 * n * n))
            assertEquals(expected, result.values.value, 1e-9)
        }
    }

    @Test fun overlayShowsAllCountsAndCullsOffscreenPartitions() {
        val overOldLimit = assertNotNull(visiblePartitionRange(-5.0, 5.0, 121, -5.0, 5.0, 48.0))
        assertEquals(0, overOldLimit.start)
        assertEquals(121, overOldLimit.endExclusive)
        assertTrue(!overOldLimit.dense)

        val dense = assertNotNull(visiblePartitionRange(-5.0, 5.0, 20_000, -5.0, 5.0, 48.0))
        assertEquals(0, dense.start)
        assertEquals(20_000, dense.endExclusive)
        assertTrue(dense.dense)

        val zoomed = assertNotNull(visiblePartitionRange(-5.0, 5.0, 20_000, -0.1, 0.1, 5000.0))
        assertTrue(!zoomed.dense)
        assertTrue(zoomed.endExclusive - zoomed.start < 500)

        assertNull(visiblePartitionRange(-5.0, 5.0, 200, 6.0, 7.0, 48.0))
    }

    @Test fun parallelAndSequentialHaveSameAnswer() = runBlocking {
        val curve = CurveDefinition(CurveKind.NEWTON, listOf(GraphPoint(0.0, 0.0), GraphPoint(2.0, 2.0)), "")
        val sequential = runIntegral(curve, 0.0, 2.0, 250, IntegralMethod.TRAPEZOID, ExecutionMode.SEQUENTIAL)
        val parallel = runIntegral(curve, 0.0, 2.0, 250, IntegralMethod.TRAPEZOID, ExecutionMode.PARALLEL)
        assertEquals(2.0, sequential.values.value, 1e-10)
        assertEquals(sequential.values.value, parallel.values.value, 1e-10)
    }
}
