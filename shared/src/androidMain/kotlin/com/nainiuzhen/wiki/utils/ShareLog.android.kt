package com.nainiuzhen.wiki.utils

import android.content.Intent
import androidx.core.content.FileProvider
import com.nainiuzhen.wiki.data.source.AppContextHolder
import java.io.File

/**
 * Android 端系统分享：把内存环形缓冲日志写到 cacheDir/logs/app_log.txt，
 * 经 FileProvider 以 ACTION_SEND 分享（存文件 / QQ / 微信 / 邮件等由用户选）。
 */
actual fun ShareDiagnosticsLog(): Boolean = try {
    val context = AppContextHolder.current
    val dir = File(context.cacheDir, "logs").apply { mkdirs() }
    val file = File(dir, "app_log.txt")
    file.writeText(AppLog.readLog())
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "奶牛镇百科 诊断日志")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(send, "分享诊断日志")
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(chooser)
    AppLog.i("诊断日志分享已唤起（${file.length()} 字节）")
    true
} catch (t: Throwable) {
    AppLog.e("诊断日志分享失败", t)
    false
}
