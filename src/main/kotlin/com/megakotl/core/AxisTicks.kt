package com.megakotl.core

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/** Automatically selects a 1/2/5 × 10^k ruler step around 85 px apart. */
fun axisTickStep(scale: Double): Double {
    require(scale > 0 && scale.isFinite())
    val rough = 85.0 / scale
    val power = 10.0.pow(floor(log10(rough)))
    return listOf(1.0, 2.0, 5.0, 10.0).first { power * it >= rough } * power
}

/** Avoid -0 and scientific notation for the zoom levels of this canvas. */
fun axisTickLabel(value: Double, step: Double): String {
    if (kotlin.math.abs(value) < step * 0.0001) return "0"
    val precision = ceil(-log10(step)).toInt().coerceIn(0, 8)
    val text = String.format(java.util.Locale.US, "%.${precision}f", value)
    return if (precision == 0) text else text.trimEnd('0').trimEnd('.')
}

