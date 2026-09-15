package com.nainiuzhen.wiki.utils

/**
 * 跨平台诊断日志（iOS 排障用，Android 侧等价落到 logcat）。
 *
 * - Android：`Log.d/e`（tag = `Wiki`），`adb logcat` 可看；
 * - iOS：写入 `Documents/app_log.txt`（Info.plist 已开 UIFileSharingEnabled，
 *   可用爱思助手 / iMazing / Filza 取出），并挂载未捕获异常钩子。
 *
 * 平台实现内部必须自行吞掉一切异常：**日志绝不能影响主流程**。
 */
object AppLog {
    fun i(msg: String) = platformLog("I", msg)

    fun e(msg: String, throwable: Throwable? = null) {
        val detail = throwable?.let { t ->
            val stack = t.stackTraceToString().lineSequence().take(15).joinToString(" ¶ ")
            " | ${t} | $stack"
        } ?: ""
        platformLog("E", msg + detail)
    }
}

/** 平台日志落地：Android → logcat；iOS → Documents/app_log.txt。 */
internal expect fun platformLog(level: String, msg: String)
