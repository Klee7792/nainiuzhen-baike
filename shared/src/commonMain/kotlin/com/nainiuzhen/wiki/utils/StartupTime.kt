package com.nainiuzhen.wiki.utils

/**
 * 冷启动计时：返回「进程启动（≈ 用户点击桌面图标）到现在」的毫秒数。
 *
 * 用于启动加载完成后在主页弹一次 `启动耗时 X.XX 秒`。
 * Android 端取 `android.os.Process.getStartElapsedRealtime()`（进程创建时刻，比
 * Application.onCreate 更贴近「点击图标」），不支持时返回 -1（调用方据此跳过 toast）。
 *
 * ⚠️ 只在**冷启动**（本次真的跑了加载流程）才有意义：进程已存活时这个值会是
 * 「进程活了多久」，可能几小时，所以调用方必须自己判断是否刚加载完。
 */
expect fun appStartElapsedMs(): Long
