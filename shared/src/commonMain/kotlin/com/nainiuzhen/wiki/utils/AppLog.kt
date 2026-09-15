package com.nainiuzhen.wiki.utils

/**
 * 跨平台诊断日志（iOS 排障用，Android 侧等价落到 logcat）。
 *
 * - Android：`Log.d/e`（tag = `Wiki`），`adb logcat` 可看；
 * - iOS：写入 `Documents/app_log.txt`（Info.plist 已开 UIFileSharingEnabled，
 *   可用爱思助手 / iMazing / Filza 取出），并挂载未捕获异常钩子。
 *
 * 除落地到平台通道外，本类还在**进程内存**里保留最近 [MAX_LOG_LINES] 行
 * （[readLog]），供设置页「诊断日志」直接展示 —— 无 Mac 时这是最省事的取日志方式
 * （截屏即可，不必连电脑导出 Documents）。
 *
 * 关键设计：**不论成功失败都要留痕**。素材「没报错但也没加载出来」是最难查的一类
 * 故障，所以各加载环节在成功路径上也必须调用 [i] 记录条目数 / 帧数，
 * 任一项为 0 时用 [w]/[e] 显式告警。
 *
 * 平台实现内部必须自行吞掉一切异常：**日志绝不能影响主流程**。
 */
object AppLog {
    fun i(msg: String) = record("I", msg)

    /** 警告：不一定致命，但结果可疑（如某份数据条数为 0）。 */
    fun w(msg: String) = record("W", msg)

    fun e(msg: String, throwable: Throwable? = null) {
        val detail = throwable?.let { t ->
            val stack = t.stackTraceToString().lineSequence().take(15).joinToString(" ¶ ")
            " | ${t} | $stack"
        } ?: ""
        record("E", msg + detail)
    }

    /**
     * 内存日志全文（按时间顺序，最多最近 [MAX_LOG_LINES] 行）。
     * 用于页内展示；失败时返回空串。
     */
    fun readLog(): String =
        try {
            recent.joinToString("\n")
        } catch (_: Throwable) {
            ""
        }

    /** 内存日志最多保留的行数（超出丢最早的）。 */
    private const val MAX_LOG_LINES = 400

    private val recent = ArrayList<String>(MAX_LOG_LINES)

    private fun record(level: String, msg: String) {
        // 时间戳取「进程启动至今」的毫秒：与冷启动耗时同源，便于判断卡在哪一段。
        val elapsed = try {
            appStartElapsedMs()
        } catch (_: Throwable) {
            -1L
        }
        val stamp = if (elapsed >= 0) "+${elapsed}ms" else "?"
        val line = "[$level $stamp] $msg"
        try {
            if (recent.size >= MAX_LOG_LINES) recent.removeAt(0)
            recent.add(line)
        } catch (_: Throwable) {
            // 内存缓冲异常不影响落地
        }
        platformLog(line)
    }
}

/** 平台日志落地：Android → logcat；iOS → Documents/app_log.txt。行内已含级别与时间戳。 */
internal expect fun platformLog(line: String)
