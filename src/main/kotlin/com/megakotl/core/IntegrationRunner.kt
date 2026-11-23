package com.megakotl.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlin.math.min

enum class ExecutionMode(val title: String) {
    SEQUENTIAL("Последовательно"), PARALLEL("Корутины")
}

data class TimedIntegral(val values: IntegralValues, val millis: Double, val workers: Int)

/** Executes both modes off the UI thread. Parallel workers NEVER share an mXparser Argument. */
suspend fun runIntegral(
    curve: CurveDefinition,
    a: Double,
    b: Double,
    n: Int,
    method: IntegralMethod,
    mode: ExecutionMode
): TimedIntegral {
    val start = System.nanoTime()
    val workers = if (mode == ExecutionMode.PARALLEL) {
        min(n, min(8, Runtime.getRuntime().availableProcessors().coerceAtLeast(1)))
    } else 1
    val partial = if (mode == ExecutionMode.SEQUENTIAL || workers == 1) {
        withContext(Dispatchers.Default) {
            val eval = requireNotNull(curve.newEvaluator())
            integratePartition(eval, a, b, n, method) { coroutineContext.ensureActive() }
        }
    } else {
        coroutineScope {
            (0 until workers).map { worker ->
                async(Dispatchers.Default) {
                    val evaluator = requireNotNull(curve.newEvaluator())
                    val begin = n * worker / workers
                    val end = n * (worker + 1) / workers
                    integratePartition(evaluator, a, b, n, method, begin, end) {
                        coroutineContext.ensureActive()
                    }
                }
            }.awaitAll().fold(PartialSums(0.0, 0.0, 0.0)) { acc, p -> acc + p }
        }
    }
    val millis = (System.nanoTime() - start) / 1_000_000.0
    return TimedIntegral(resultOf(partial, method), millis, workers)
}
