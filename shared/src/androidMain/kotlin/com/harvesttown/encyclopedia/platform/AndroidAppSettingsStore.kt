package com.harvesttown.encyclopedia.platform

import android.content.Context
import com.harvesttown.encyclopedia.utils.AppSettingsStore
import com.harvesttown.encyclopedia.utils.AppState

/**
 * 基于 `SharedPreferences` 的 [AppSettingsStore] 实现（Android 端）。
 *
 * 文件：`getSharedPreferences("app_settings", MODE_PRIVATE)`。
 * 仅持久化标量偏好（深色模式 / 圆角 / 模糊 / 转场样式 / 滑动返回），
 * 保证重启后设置不丢失（此前深色模式不记忆即源于此）。
 */
class AndroidAppSettingsStore(context: Context) : AppSettingsStore {
    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    override fun load(): AppState = AppState(
        isDark = prefs.getBoolean(KEY_DARK, false),
        enableBlur = prefs.getBoolean(KEY_BLUR, true),
        enableSquircle = prefs.getBoolean(KEY_SQUIRCLE, true),
        navTransitionStyle = prefs.getInt(KEY_TRANSITION, 0),
        enableSwipeBack = prefs.getBoolean(KEY_SWIPE_BACK, true),
    )

    override fun save(state: AppState) {
        prefs.edit().apply {
            putBoolean(KEY_DARK, state.isDark)
            putBoolean(KEY_BLUR, state.enableBlur)
            putBoolean(KEY_SQUIRCLE, state.enableSquircle)
            putInt(KEY_TRANSITION, state.navTransitionStyle)
            putBoolean(KEY_SWIPE_BACK, state.enableSwipeBack)
            apply()
        }
    }

    private companion object {
        const val KEY_DARK = "isDark"
        const val KEY_BLUR = "enableBlur"
        const val KEY_SQUIRCLE = "enableSquircle"
        const val KEY_TRANSITION = "navTransitionStyle"
        const val KEY_SWIPE_BACK = "enableSwipeBack"
    }
}
