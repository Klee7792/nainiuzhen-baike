package com.harvesttown.encyclopedia.utils

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.WindowInsetsController
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

/**
 * Android 实现：根据 [light] 切换状态栏图标深浅。
 * 使用平台 WindowInsetsController / SYSTEM_UI_FLAG（不依赖 androidx.core，避免新增依赖）。
 */
@Composable
actual fun SetStatusBarLightIcons(light: Boolean) {
    val view = LocalView.current
    DisposableEffect(light, view) {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            val decor = window.decorView
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.setSystemBarsAppearance(
                    if (light) WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS else 0,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                )
            } else {
                @Suppress("DEPRECATION")
                decor.systemUiVisibility = if (light) {
                    decor.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                } else {
                    decor.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                }
            }
        }
        onDispose { }
    }
}
