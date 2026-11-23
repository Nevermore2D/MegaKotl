package com.megakotl.core

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.abs

/** Automatically selects a 1/2/5 × 10^k ruler step around 85 px apart. */
fun axisTickStep(scale: Double): Double {
    require(scale > 0 && scale.isFinite())
    val rough = 85.0 / scale
    val power = 10.0.pow(floor(log10(rough)))
    return listOf(1.0, 2.0, 5.0, 10.0).first { power * it >= rough } * power
}

/** Human-readable decimal formatting on JVM and Wasm without Java String.format. */
fun decimalLabel(value: Double, digits: Int): String {
    if (!value.isFinite()) return "—"
    val precision = digits.coerceIn(0, 8)
    val factor = 10.0.pow(precision)
    val scaled = round(value * factor)
    if (abs(scaled) >= Long.MAX_VALUE.toDouble() / 2) return value.toString()
    val integer = scaled.toLong()
    if (integer == 0L) return "0"
    val negative = integer < 0
    val positive = if (negative) -integer else integer
    val divisor = factor.toLong()
    val whole = positive / divisor
    val fraction = (positive % divisor).toString().padStart(precision, '0').trimEnd('0')
    return (if (negative) "-" else "") + whole.toString() +
        (if (fraction.isEmpty()) "" else ".$fraction")
}

/** Axis ticks are rounded according to the currently chosen grid step. */
fun axisTickLabel(value: Double, step: Double): String {
    if (abs(value) < step * 0.0001) return "0"
    return decimalLabel(value, ceil(-log10(step)).toInt().coerceIn(0, 8))
}
