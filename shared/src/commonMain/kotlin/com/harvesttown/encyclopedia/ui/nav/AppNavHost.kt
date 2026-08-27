package com.harvesttown.encyclopedia.ui.nav

import androidx.compose.runtime.Composable
import com.harvesttown.encyclopedia.ui.home.MainScreen
import com.harvesttown.encyclopedia.ui.items.ItemListScreen
import com.harvesttown.encyclopedia.ui.npc.NpcListScreen
import com.harvesttown.encyclopedia.ui.npc.NpcScheduleScreen
import com.harvesttown.encyclopedia.ui.recipe.RecipeListScreen
import com.harvesttown.encyclopedia.utils.LocalAppSettings
import top.yukonga.miuix.kmp.nav.core.NavBackStack
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.transition.NavTransitions

/**
 * 路由表：将 [Route] 各目的地映射到对应页面，统一使用 miuix-nav 过渡与效果。
 * 过渡样式（Miuix / AOSP）跟随设置页偏好。
 */
@Composable
fun AppNavHost(
    backStack: NavBackStack,
    navigator: Navigator,
) {
    val appState = LocalAppSettings.current
    val transition = if (appState.navTransitionStyle == 1) NavTransitions.Modal else NavTransitions.MiuixDefault
    NavDisplay(
        backStack = backStack,
        onBack = { navigator.pop() },
        transition = transition,
        effects = NavDisplayEffects(),
    ) {
        entry<Route.Main> {
            MainScreen()
        }
        entry<Route.ItemList> {
            ItemListScreen()
        }
        entry<Route.RecipeList> {
            RecipeListScreen()
        }
        entry<Route.NpcList> {
            NpcListScreen()
        }
        entry<Route.NpcSchedule> { route ->
            NpcScheduleScreen(npcId = route.npcId)
        }
    }
}
