package com.nainiuzhen.wiki.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNotificationName
import platform.Foundation.NSOperationQueue

/**
 * iOS 实现：监听 `UIApplicationDidBecomeActiveNotification` 触发 [onResume]
 * （对应 Android 的 Lifecycle ON_RESUME：前台激活即回前台）。
 *
 * 注：K/N 绑定未导出 `UIApplication.didBecomeActiveNotification` 扩展属性，
 * 改为按通知名字符串手动构造（该名字为苹果公开稳定常量，行为等价）。
 */
@Composable
actual fun OnResumeEffect(onResume: () -> Unit) {
    val latestOnResume by rememberUpdatedState(onResume)
    DisposableEffect(Unit) {
        val center = NSNotificationCenter.defaultCenter
        val observer = center.addObserverForName(
            name = NSNotificationName("UIApplicationDidBecomeActiveNotification"),
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) { _ ->
            latestOnResume()
        }
        onDispose { center.removeObserver(observer) }
    }
}
