package com.megakotl.workspace

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt

/** UI-only logarithmic slider. The integration kernel always receives the exact n. */
internal const val MAX_PARTITIONS = 20_000
private val logMaxPartitions = ln(MAX_PARTITIONS.toDouble())

internal fun partitionsToSlider(n: Int): Float =
    (ln(n.coerceIn(1, MAX_PARTITIONS).toDouble()) / logMaxPartitions).toFloat()

internal fun sliderToPartitions(position: Float): Int =
    exp(position.coerceIn(0f, 1f).toDouble() * logMaxPartitions)
        .roundToInt().coerceIn(1, MAX_PARTITIONS)
