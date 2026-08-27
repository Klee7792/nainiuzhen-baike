package com.harvesttown.encyclopedia.ui.nav

import kotlinx.serialization.Serializable
import top.yukonga.miuix.kmp.nav.core.NavKey

/**
 * 类型安全的导航路由（基于 miuix-nav）。每个目的地都是一个 [NavKey]，可保存/恢复于返回栈，
 * 因此可在页面旋转等场景下正确重建。
 */
@Serializable
sealed interface Route : NavKey {
    /** 主页。 */
    @Serializable
    data object Home : Route

    /** 物品大全。 */
    @Serializable
    data object ItemList : Route

    /** 配方查询。 */
    @Serializable
    data object RecipeList : Route

    /** NPC 资料。 */
    @Serializable
    data object NpcList : Route

    /** 设置。 */
    @Serializable
    data object Settings : Route

    /** NPC 日程子页（携带 npcId）。 */
    @Serializable
    data class NpcSchedule(val npcId: Int) : Route
}
