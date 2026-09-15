package com.nainiuzhen.wiki.ui.nav

import kotlinx.serialization.Serializable
import top.yukonga.miuix.kmp.nav.core.NavKey

/**
 * 类型安全的导航路由（基于 miuix-nav）。每个目的地都是一个 [NavKey]，可保存/恢复于返回栈，
 * 因此可在页面旋转等场景下正确重建。
 *
 * 结构（底栏 2 页）：
 * - [Main] 主页（含 3 个图鉴板块入口：物品大全 / 配方查询 / NPC 资料）+ 设置页并存的宿主。
 * - 子页（从主页入口 push 而来，全屏覆盖）：[ItemList] / [RecipeList] / [NpcList] / [NpcSchedule]。
 * - [About] 关于子页（从设置页「关于」进入，紫→蓝渐变）。
 */
@Serializable
sealed interface Route : NavKey {
    /** 主页（底栏第 1 页）。 */
    @Serializable
    data object Main : Route

    /** 物品大全。 */
    @Serializable
    data object ItemList : Route

    /** 配方查询。 */
    @Serializable
    data object RecipeList : Route

    /** NPC 资料。 */
    @Serializable
    data object NpcList : Route

    /** NPC 日程子页（携带 npcId）。 */
    @Serializable
    data class NpcSchedule(val npcId: Int) : Route

    /** 关于子页。 */
    @Serializable
    data object About : Route

    /** 第三方开源协议子页（从关于页「第三方开源协议」进入）。 */
    @Serializable
    data object License : Route

    /** 图片放大倍率设置子页（从设置页「图片放大倍率」进入）。 */
    @Serializable
    data object ImageScaleSettings : Route

    /** 卡片设置子页（从设置页「卡片设置」进入；背景 / 圆角 / 名称胶囊）。 */
    @Serializable
    data object CardSettings : Route

    /** 诊断信息子页（从设置页「诊断日志」进入；数据概况 + 运行日志）。 */
    @Serializable
    data object Diagnostics : Route
}
