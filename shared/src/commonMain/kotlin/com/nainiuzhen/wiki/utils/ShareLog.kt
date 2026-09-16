package com.nainiuzhen.wiki.utils

/**
 * 调起系统分享面板分享诊断日志文件：
 * - iOS：UIActivityViewController（存到「文件」/ QQ / 微信 / 隔空投送 / 邮件等），
 *   优先分享落盘的完整日志 Documents/app_log.txt（跨会话累积）；
 * - Android：ACTION_SEND + FileProvider（存文件 / 发给 QQ、微信等）。
 *
 * 必须在主线程调用（iOS present 层级要求）。返回是否成功唤起（失败已记日志）。
 */
expect fun ShareDiagnosticsLog(): Boolean
