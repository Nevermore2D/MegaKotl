package com.megakotl.core

/** Platform boundary: JVM uses mXparser; WebAssembly uses the Kotlin parser. */
internal expect fun formulaSyntaxError(expression: String): String?

/** Each call creates a private, stateful evaluator for safe coroutine partitioning. */
internal expect fun createFormulaEvaluator(expression: String): (Double) -> Double
