package com.nainiuzhen.wiki.utils

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

/**
 * Android 实现：通过宿主的 [Activity] 应用屏幕方向请求。
 *
 * 沿用工程内既有的平台副作用写法（同 `StatusBar.android.kt` / `OnResumeEffect.android.kt`）：
 * 从 [LocalView] 取 context 并上溯到 [Activity]，不新造全局单例。
 * 依赖由 miuix 传递引入，无需新增依赖。
 *
 * @param allowLandscape `true` → [ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED]（自由旋转）；
 *                       `false` → [ActivityInfo.SCREEN_ORIENTATION_PORTRAIT]（锁竖屏）。
 */
@Composable
actual fun ApplyPhoneOrientation(allowLandscape: Boolean) {
    val view = LocalView.current
    // key 含 allowLandscape：首帧执行一次 + 开关变化时立即重跑，正好覆盖「启动应用一次 + 切换立即应用」。
    DisposableEffect(allowLandscape, view) {
        val activity = view.context as? Activity
        activity?.setRequestedOrientation(
            if (allowLandscape) {
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            } else {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            },
        )
        onDispose { }
    }
}
