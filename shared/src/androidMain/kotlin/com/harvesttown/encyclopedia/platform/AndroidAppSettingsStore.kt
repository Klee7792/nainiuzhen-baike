package com.harvesttown.encyclopedia.platform

import android.content.Context
import com.harvesttown.encyclopedia.utils.AppSettingsStore
import com.harvesttown.encyclopedia.utils.AppState

/**
 * 基于 `SharedPreferences` 的 [AppSettingsStore] 实现（Android 端）。
 *
 * 文件：`getSharedPreferences("app_settings", MODE_PRIVATE)`。
 * 持久化全部标量偏好（主题 colorMode/monet、圆角/模糊/转场/滑动返回，以及 v5 新增的 14 个开关），
 * 保证重启后设置不丢失。
 */
class AndroidAppSettingsStore(context: Context) : AppSettingsStore {
    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    override fun load(): AppState = AppState(
        colorMode = prefs.getInt(KEY_COLOR_MODE, 0),
        monet = prefs.getBoolean(KEY_MONET, false),
        enableBlur = prefs.getBoolean(KEY_BLUR, true),
        enableSquircle = prefs.getBoolean(KEY_SQUIRCLE, true),
        navTransitionStyle = prefs.getInt(KEY_TRANSITION, 0),
        enableSwipeBack = prefs.getBoolean(KEY_SWIPE_BACK, true),
        enableCornerClip = prefs.getBoolean(KEY_CORNER_CLIP, true),
        scrollEndHaptic = prefs.getBoolean(KEY_SCROLL_HAPTIC, false),
        pageUserScroll = prefs.getBoolean(KEY_PAGE_SCROLL, true),
        showTopAppBar = prefs.getBoolean(KEY_SHOW_TOP_BAR, true),
        topAppBarBlurStyle = prefs.getInt(KEY_TOP_BAR_BLUR_STYLE, 0),
        showNavigationBar = prefs.getBoolean(KEY_SHOW_NAV_BAR, true),
        showNavigationBadge = prefs.getBoolean(KEY_SHOW_NAV_BADGE, true),
        navigationBarMode = prefs.getInt(KEY_NAV_BAR_MODE, 0),
        useFloatingNavigationBar = prefs.getBoolean(KEY_FLOATING_NAV_BAR, false),
        showFloatingToolbar = prefs.getBoolean(KEY_FLOATING_TOOLBAR, false),
        floatingToolbarPosition = prefs.getInt(KEY_FLOATING_TOOLBAR_POS, 0),
        showFloatingActionButton = prefs.getBoolean(KEY_FLOATING_FAB, false),
        floatingActionButtonPosition = prefs.getInt(KEY_FLOATING_FAB_POS, 0),
        enableDim = prefs.getBoolean(KEY_DIM, false),
        blockInputDuringTransition = prefs.getBoolean(KEY_BLOCK_INPUT, false),
        floatingNavigationBarStyle = prefs.getInt(KEY_FLOATING_NAV_BAR_STYLE, 0),
        floatingNavigationBarPosition = prefs.getInt(KEY_FLOATING_NAV_BAR_POS, 0),
    )

    override fun save(state: AppState) {
        prefs.edit().apply {
            putInt(KEY_COLOR_MODE, state.colorMode)
            putBoolean(KEY_MONET, state.monet)
            putBoolean(KEY_BLUR, state.enableBlur)
            putBoolean(KEY_SQUIRCLE, state.enableSquircle)
            putInt(KEY_TRANSITION, state.navTransitionStyle)
            putBoolean(KEY_SWIPE_BACK, state.enableSwipeBack)
            putBoolean(KEY_CORNER_CLIP, state.enableCornerClip)
            putBoolean(KEY_SCROLL_HAPTIC, state.scrollEndHaptic)
            putBoolean(KEY_PAGE_SCROLL, state.pageUserScroll)
            putBoolean(KEY_SHOW_TOP_BAR, state.showTopAppBar)
            putInt(KEY_TOP_BAR_BLUR_STYLE, state.topAppBarBlurStyle)
            putBoolean(KEY_SHOW_NAV_BAR, state.showNavigationBar)
            putBoolean(KEY_SHOW_NAV_BADGE, state.showNavigationBadge)
            putInt(KEY_NAV_BAR_MODE, state.navigationBarMode)
            putBoolean(KEY_FLOATING_NAV_BAR, state.useFloatingNavigationBar)
            putBoolean(KEY_FLOATING_TOOLBAR, state.showFloatingToolbar)
            putInt(KEY_FLOATING_TOOLBAR_POS, state.floatingToolbarPosition)
            putBoolean(KEY_FLOATING_FAB, state.showFloatingActionButton)
            putInt(KEY_FLOATING_FAB_POS, state.floatingActionButtonPosition)
            putBoolean(KEY_DIM, state.enableDim)
            putBoolean(KEY_BLOCK_INPUT, state.blockInputDuringTransition)
            putInt(KEY_FLOATING_NAV_BAR_STYLE, state.floatingNavigationBarStyle)
            putInt(KEY_FLOATING_NAV_BAR_POS, state.floatingNavigationBarPosition)
            apply()
        }
    }

    private companion object {
        const val KEY_COLOR_MODE = "colorMode"
        const val KEY_MONET = "monet"
        const val KEY_BLUR = "enableBlur"
        const val KEY_SQUIRCLE = "enableSquircle"
        const val KEY_TRANSITION = "navTransitionStyle"
        const val KEY_SWIPE_BACK = "enableSwipeBack"
        const val KEY_CORNER_CLIP = "enableCornerClip"
        const val KEY_SCROLL_HAPTIC = "scrollEndHaptic"
        const val KEY_PAGE_SCROLL = "pageUserScroll"
        const val KEY_SHOW_TOP_BAR = "showTopAppBar"
        const val KEY_TOP_BAR_BLUR_STYLE = "topAppBarBlurStyle"
        const val KEY_SHOW_NAV_BAR = "showNavigationBar"
        const val KEY_SHOW_NAV_BADGE = "showNavigationBadge"
        const val KEY_NAV_BAR_MODE = "navigationBarMode"
        const val KEY_FLOATING_NAV_BAR = "useFloatingNavigationBar"
        const val KEY_FLOATING_TOOLBAR = "showFloatingToolbar"
        const val KEY_FLOATING_TOOLBAR_POS = "floatingToolbarPosition"
        const val KEY_FLOATING_FAB = "showFloatingActionButton"
        const val KEY_FLOATING_FAB_POS = "floatingActionButtonPosition"
        const val KEY_DIM = "enableDim"
        const val KEY_BLOCK_INPUT = "blockInputDuringTransition"
        const val KEY_FLOATING_NAV_BAR_STYLE = "floatingNavigationBarStyle"
        const val KEY_FLOATING_NAV_BAR_POS = "floatingNavigationBarPosition"
    }
}
