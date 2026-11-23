package com.megakotl.core

/** JVM can use multiple Dispatchers.Default threads. Browser/Wasm is single-threaded here. */
internal expect fun computationalWorkerCount(): Int

/** Text visible in the shared UI, reflecting actual execution on the target platform. */
internal expect fun coroutineModeInfo(): String
