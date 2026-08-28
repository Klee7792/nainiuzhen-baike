package com.harvesttown.encyclopedia.utils

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/**
 * Android 实现：通过 [ComponentActivity] 的 [Lifecycle] 监听 ON_RESUME 事件触发 [onResume]。
 * 依赖 androidx.activity / androidx.lifecycle（均由 miuix 传递引入，无需新增依赖）。
 */
@Composable
actual fun OnResumeEffect(onResume: () -> Unit) {
    val view = LocalView.current
    DisposableEffect(view) {
        val activity = view.context as? ComponentActivity
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) onResume()
        }
        activity?.lifecycle?.addObserver(observer)
        onDispose { activity?.lifecycle?.removeObserver(observer) }
    }
}
