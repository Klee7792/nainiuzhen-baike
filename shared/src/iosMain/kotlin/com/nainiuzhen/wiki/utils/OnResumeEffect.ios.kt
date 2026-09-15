package com.nainiuzhen.wiki.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * iOS 实现：监听 Compose Multiplatform 自动管理的 [Lifecycle] 的 ON_RESUME 事件触发 [onResume]。
 *
 * CMP 在 iOS 上自动把 `UIApplication` 生命周期桥接进 `LocalLifecycleOwner`
 * （didBecomeActive → ON_RESUME 等），行为等价于 Android 端监听 Activity Lifecycle。
 * 相比直接订阅 `NSNotificationCenter`：无需构造 NSNotificationName（K/N 未导出
 * 字符串构造器，扩展通知常量也未导出），且与跨平台 Lifecycle 语义完全对齐。
 */
@Composable
actual fun OnResumeEffect(onResume: () -> Unit) {
    val owner = LocalLifecycleOwner.current
    val latestOnResume by rememberUpdatedState(onResume)
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) latestOnResume()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
}
