package com.nainiuzhen.wiki.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplication

/**
 * iOS 实现：监听 `UIApplication.didBecomeActiveNotification` 触发 [onResume]
 * （对应 Android 的 Lifecycle ON_RESUME：前台激活即回前台）。
 */
@Composable
actual fun OnResumeEffect(onResume: () -> Unit) {
    val latestOnResume by rememberUpdatedState(onResume)
    DisposableEffect(Unit) {
        val center = NSNotificationCenter.defaultCenter
        val observer = center.addObserverForName(
            name = UIApplication.didBecomeActiveNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) { _ ->
            latestOnResume()
        }
        onDispose { center.removeObserver(observer) }
    }
}
