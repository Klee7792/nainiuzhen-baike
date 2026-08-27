package com.harvesttown.encyclopedia.ui.nav

import androidx.compose.runtime.Composable
import com.harvesttown.encyclopedia.ui.home.HomeScreen
import com.harvesttown.encyclopedia.ui.items.ItemListScreen
import com.harvesttown.encyclopedia.ui.npc.NpcListScreen
import com.harvesttown.encyclopedia.ui.npc.NpcScheduleScreen
import com.harvesttown.encyclopedia.ui.recipe.RecipeListScreen
import com.harvesttown.encyclopedia.ui.settings.SettingsScreen
import top.yukonga.miuix.kmp.nav.core.NavBackStack
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.transition.NavTransitions

/**
 * 路由表：将 [Route] 各目的地映射到对应页面，统一使用 miuix-nav 默认过渡与效果。
 */
@Composable
fun AppNavHost(
    backStack: NavBackStack,
    navigator: Navigator,
) {
    NavDisplay(
        backStack = backStack,
        onBack = { navigator.pop() },
        transition = NavTransitions.MiuixDefault,
        effects = NavDisplayEffects(),
    ) {
        entry<Route.Home> {
            HomeScreen()
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
        entry<Route.Settings> {
            SettingsScreen()
        }
        entry<Route.NpcSchedule> { route ->
            NpcScheduleScreen(npcId = route.npcId)
        }
    }
}
