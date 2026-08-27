package com.harvesttown.encyclopedia.data.repository

import com.harvesttown.encyclopedia.data.model.ItemInfo
import com.harvesttown.encyclopedia.data.model.NpcInfo
import com.harvesttown.encyclopedia.data.model.NpcSchedule
import com.harvesttown.encyclopedia.data.model.RecipeInfo

/**
 * 数据仓储：聚合物品 / 配方 / NPC / 日程，并提供按分类、按 id 的查询。
 *
 * 黑名单（release 过滤、debug 展示）已在 [com.harvesttown.encyclopedia.data.AssetManager]
 * 构建时过滤，这里保存原始黑名单集合供需要处使用。
 */
class DataRepository(
    val items: List<ItemInfo>,
    val recipes: List<RecipeInfo>,
    val npcs: List<NpcInfo>,
    val schedules: Map<Int, List<NpcSchedule>>,
    val itemBlacklist: Set<Int>,
    val npcBlacklist: Set<Int>,
) {
    /** 物品按分类标签过滤（空集合 = 全部）。 */
    fun itemsByCategory(labels: Set<String>): List<ItemInfo> =
        if (labels.isEmpty()) items else items.filter { it.categoryLabel in labels }

    /** 配方按类型标签过滤（空集合 = 全部）。 */
    fun recipesByCategory(labels: Set<String>): List<RecipeInfo> =
        if (labels.isEmpty()) recipes else recipes.filter { it.typeLabel in labels }

    /** 按 id 查物品。 */
    fun itemById(id: Int): ItemInfo? = items.firstOrNull { it.id == id }

    /** 按 id 查配方。 */
    fun recipeById(id: Int): RecipeInfo? = recipes.firstOrNull { it.id == id }

    /** 按 id 查 NPC。 */
    fun npcById(id: Int): NpcInfo? = npcs.firstOrNull { it.id == id }

    /** 查某 NPC 的全部日程。 */
    fun npcSchedules(npcId: Int): List<NpcSchedule> = schedules[npcId] ?: emptyList()
}
