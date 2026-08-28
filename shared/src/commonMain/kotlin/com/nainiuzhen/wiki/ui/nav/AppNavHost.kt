package com.nainiuzhen.wiki.ui.nav

import androidx.compose.runtime.Composable
import com.nainiuzhen.wiki.ui.home.MainScreen
import com.nainiuzhen.wiki.ui.items.ItemListScreen
import com.nainiuzhen.wiki.ui.npc.NpcListScreen
import com.nainiuzhen.wiki.ui.npc.NpcScheduleScreen
import com.nainiuzhen.wiki.ui.recipe.RecipeListScreen
import com.nainiuzhen.wiki.ui.settings.AboutScreen
import com.nainiuzhen.wiki.utils.LocalAppSettings
import top.yukonga.miuix.kmp.nav.core.NavBackStack
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection
import top.yukonga.miuix.kmp.nav.transition.NavTransitions

/**
 * 路由表：将 [Route] 各目的地映射到对应页面，统一使用 miuix-nav 过渡与效果。
 * 过渡样式（Miuix / Modal）、圆角裁剪、遮罩变暗、转场拦截输入均跟随设置页偏好（变更点 #23/#26）。
 */
@Composable
fun AppNavHost(
    backStack: NavBackStack,
    navigator: Navigator,
) {
    val appState = LocalAppSettings.current
    // 过渡动画：navTransitionStyle == 1 → Modal，否则 MiuixDefault（pit #4：无 AOSP）。
    val transition = if (appState.navTransitionStyle == 1) NavTransitions.Modal else NavTransitions.MiuixDefault
    // 正交转场效果：圆角裁剪 / 遮罩变暗 / 转场拦截输入跟随开关。
    val effects = NavDisplayEffects(
        enableCornerClip = appState.enableCornerClip,
        dimAmount = if (appState.enableDim) 0.5f else 0f,
        blockInputDuringTransition = appState.blockInputDuringTransition,
    )
    // 滑动返回：关闭开关时显式禁用，否则继承过渡默认方向。
    val swipe = if (appState.enableSwipeBack) null else NavSwipeDirection.None
    NavDisplay(
        backStack = backStack,
        onBack = { navigator.pop() },
        transition = transition,
        effects = effects,
    ) {
        entry<Route.Main> {
            MainScreen()
        }
        entry<Route.ItemList>(swipeDismiss = swipe) {
            ItemListScreen()
        }
        entry<Route.RecipeList>(swipeDismiss = swipe) {
            RecipeListScreen()
        }
        entry<Route.NpcList>(swipeDismiss = swipe) {
            NpcListScreen()
        }
        entry<Route.NpcSchedule>(swipeDismiss = swipe) { route ->
            NpcScheduleScreen(npcId = route.npcId)
        }
        entry<Route.About>(swipeDismiss = swipe) {
            AboutScreen()
        }
    }
}
