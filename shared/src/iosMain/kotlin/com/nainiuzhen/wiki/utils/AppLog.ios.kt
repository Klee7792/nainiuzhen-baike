@file:OptIn(ExperimentalForeignApi::class)

package com.nainiuzhen.wiki.utils

import kotlin.concurrent.Volatile
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fputs

/**
 * iOS 诊断日志落地：追加写 `Documents/app_log.txt`。
 *
 * 只用 POSIX stdio + `NSSearchPathForDirectoriesInDomains`（后者在本工程已编译验证），
 * 规避 cinterop 里 Foundation 各类工厂方法的导入差异。
 * 全程 try/catch：**日志失败绝不能影响启动**。
 */
private const val LOG_FILE = "app_log.txt"

private fun documentsPath(): String? =
    (NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
        .firstOrNull() as? String)

/**
 * iOS 诊断日志落地：追加写 `Documents/app_log.txt`（Filza / 爱思助手可直接取）。
 *
 * @param line 已含级别与时间戳的一整行（见 [AppLog]）。
 */
internal actual fun platformLog(line: String) {
    try {
        val dir = documentsPath() ?: return
        // 追加写：跨启动保留历史。每次进程首次写日志时插一行分隔，便于区分会话。
        val first = !sessionMarked
        if (first) sessionMarked = true
        val fp = fopen("$dir/$LOG_FILE", "a") ?: return
        try {
            if (first) fputs("\n==== 新会话 ====\n", fp)
            fputs("$line\n", fp)
        } finally {
            fclose(fp)
        }
    } catch (_: Throwable) {
        // 兜底：日志不可写时静默放弃（内存缓冲仍在，页内「诊断日志」可看）
    }
}

@Volatile
private var sessionMarked = false
