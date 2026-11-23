package com.megakotl.core

internal actual fun computationalWorkerCount(): Int =
    Runtime.getRuntime().availableProcessors().coerceAtLeast(1)

internal actual fun coroutineModeInfo(): String =
    "Корутины разбивают N интервалов между независимыми задачами в Dispatchers.Default."
