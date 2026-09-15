@file:OptIn(ExperimentalForeignApi::class)

package com.nainiuzhen.wiki.utils

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

internal actual fun platformLog(level: String, msg: String) {
    try {
        val dir = documentsPath() ?: return
        val fp = fopen("$dir/$LOG_FILE", "a") ?: return
        try {
            fputs("[$level] $msg\n", fp)
        } finally {
            fclose(fp)
        }
    } catch (_: Throwable) {
        // 兜底：日志不可写时静默放弃
    }
}
