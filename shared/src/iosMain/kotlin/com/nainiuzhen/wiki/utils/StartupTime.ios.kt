package com.nainiuzhen.wiki.utils

import kotlin.concurrent.Volatile
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.posix.CLOCK_MONOTONIC
import platform.posix.clock_gettime
import platform.posix.timespec

/**
 * iOS 端「进程启动时刻」打点与冷启动计时。
 *
 * K/N 没有等价于 Android `Process.getStartElapsedRealtime` 的进程创建时刻，
 * 且顶层属性是首次访问时才初始化（点会偏晚），故由入口 `MainViewController()`
 * 在构建 Compose 根**之前**显式调用 [IosStartupTime.mark] 打点，
 * [appStartElapsedMs] 与之相减即「启动 → 首屏内容齐备」的真实耗时。
 */
object IosStartupTime {
    @Volatile
    var startMs: Long = -1L
        private set

    /** 记录启动时刻（幂等：只在首次调用生效）。 */
    fun mark() {
        if (startMs < 0) startMs = monotonicMs()
    }
}

actual fun appStartElapsedMs(): Long {
    val start = IosStartupTime.startMs
    if (start < 0) return -1L
    val now = monotonicMs()
    return if (now >= start) now - start else -1L
}

@OptIn(ExperimentalForeignApi::class)
private fun monotonicMs(): Long = memScoped {
    val ts = alloc<timespec>()
    clock_gettime(CLOCK_MONOTONIC, ts.ptr)
    ts.tv_sec * 1000L + ts.tv_nsec / 1_000_000L
}
