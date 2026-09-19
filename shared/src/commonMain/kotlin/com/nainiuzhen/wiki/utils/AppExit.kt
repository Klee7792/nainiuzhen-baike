package com.nainiuzhen.wiki.utils

/**
 * 退出应用（「使用须知」点「不同意并退出」时调用）。
 *
 * `kotlin.system.exitProcess` 与 `platform.posix.exit` 都不是 common 声明，
 * commonMain 无法直接调用，故用 expect/actual（命名与落地文件对齐 [ApplyPhoneOrientation]）。
 */
expect fun exitApplication()
