package com.megakotl.core

// Kotlin/Wasm coroutines on the browser main thread are cooperative, not multicore.
internal actual fun computationalWorkerCount(): Int = 1

internal actual fun coroutineModeInfo(): String =
    "Web/Wasm: задачи выполняются последовательно в одном потоке браузера; " +
        "кнопка «Корутины» не ускоряет вычисление на нескольких ядрах."
