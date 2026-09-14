package com.nainiuzhen.wiki.utils

import android.os.Process
import android.os.SystemClock

/**
 * Android 端：[Process.getStartElapsedRealtime] 给出进程创建时刻（API 24+，minSdk 正好 24），
 * 与 [SystemClock.elapsedRealtime] 相减即「点击图标 → 现在」的真实耗时（含系统 fork/加载 APK 的时间）。
 * 取不到时返回 -1，调用方跳过 toast。
 */
actual fun appStartElapsedMs(): Long {
    val start = Process.getStartElapsedRealtime()
    if (start <= 0L) return -1L
    val now = SystemClock.elapsedRealtime()
    return if (now >= start) now - start else -1L
}
