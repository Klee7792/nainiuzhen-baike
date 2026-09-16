@file:OptIn(ExperimentalForeignApi::class)

package com.nainiuzhen.wiki.utils

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fputs
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.popoverPresentationController
import platform.CoreGraphics.CGRectMake

/**
 * iOS 端系统分享：UIActivityViewController（存到「文件」/ QQ / 微信 / 隔空投送 / 邮件等）。
 *
 * 分享对象 = 落盘完整日志 Documents/app_log.txt（跨会话累积，Filza 同款文件）；
 * 文件尚不存在时（首启且未满一批落盘）先把内存环形缓冲写入该路径。
 * iPad 上 UIActivityViewController 必须给 popover 锚点，否则 present 崩溃。
 */
actual fun ShareDiagnosticsLog(): Boolean = try {
    val dir = (NSFileManager.defaultManager.URLsForDirectory(
        NSDocumentDirectory, NSUserDomainMask,
    ).firstOrNull() as? NSURL)?.path ?: return false
    val path = "$dir/app_log.txt"
    val exists = NSFileManager.defaultManager.fileExistsAtPath(path)
    if (!exists) {
        // 落盘文件缺失：把环形缓冲快照写入，保证分享出去一定有内容。
        val fp = fopen(path, "w") ?: return false
        try {
            for (line in AppLog.readLog().lineSequence()) fputs("$line\n", fp)
        } finally {
            fclose(fp)
        }
    }
    val url = NSURL.fileURLWithPath(path)
    val root = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return false
    var top = root
    while (top.presentedViewController != null) {
        top = top.presentedViewController ?: break
    }
    val sheet = UIActivityViewController(
        activityItems = listOf(url),
        applicationActivities = null,
    )
    // iPad 必须锚定 popover（iPhone 上 popoverPresentationController 为 null，自动走全屏卡片）
    sheet.popoverPresentationController?.let { pop ->
        pop.sourceView = top.view
        pop.sourceRect = top.view.bounds.useContents {
            CGRectMake(origin.x + size.width / 2.0, origin.y + size.height - 1.0, 1.0, 1.0)
        }
    }
    top.presentViewController(sheet, animated = true, completion = null)
    AppLog.i("诊断日志分享已唤起（$path）")
    true
} catch (t: Throwable) {
    AppLog.e("诊断日志分享失败", t)
    false
}
