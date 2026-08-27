package com.harvesttown.encyclopedia.data.model

/**
 * 配方原料。
 *
 * @param id 原料物品 id。
 * @param num 数量。
 */
data class RecipeMaterial(
    val id: Int,
    val num: Int,
)

/**
 * 配方模型，由 `compound_unlocks.json` 的 `items[]` 解析而来。
 *
 * @param id 配方 id。
 * @param name 名称。
 * @param type 类型。
 * @param typeLabel 类型标签（如「设备打造」），用于列表分类。
 * @param category 类别（recipe_like 等）。
 * @param target 产物物品 id。
 * @param targetNum 产物数量。
 * @param materials 原料列表。
 * @param iconHint 原始 icon 提示。
 * @param deblockingDesc 解锁条件 / get_way；可为空。
 * @param descRaw 富文本描述；可为空。
 * @param price 售价；可为空。
 * @param iconFrameKey 切片帧名（不含 `.png`）。
 */
data class RecipeInfo(
    val id: Int,
    val name: String,
    val type: Int,
    val typeLabel: String,
    val category: String,
    val target: Int,
    val targetNum: Int,
    val materials: List<RecipeMaterial>,
    val iconHint: Int,
    val deblockingDesc: String?,
    val descRaw: String?,
    val price: Int?,
    val iconFrameKey: String,
)
