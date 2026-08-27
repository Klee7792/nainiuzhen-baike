package com.harvesttown.encyclopedia.data.model

/**
 * 物品模型，由 [com.harvesttown.encyclopedia.data.AssetManager] 从
 * `item_database.json` 解析而来。
 *
 * @param id 物品 id。
 * @param name 名称。
 * @param descRaw 富文本原始串（`/#颜色#内容/#`、`/#颜色#SIZE#内容/#`、`\n`）。
 * @param source 来源（`get_way`）；可为空。
 * @param iconHint 原始 `icon` 提示（可能为 null）。
 * @param type 分类类型（鱼类 / 矿石 / 加工品 …）。
 * @param price 售价（无则隐藏价格区）；可为空。
 * @param sellboxPrice 售卖箱价格；可为空。
 * @param iconFrameKey 切片帧名（不含 `.png`），由 icon_mapping / icon / id 推导。
 * @param categoryLabel 分类标签（取自富文本 `类型：XXX`，缺省为「其他」）。
 * @param group 物品分组（P1 分离用）。
 */
data class ItemInfo(
    val id: Int,
    val name: String,
    val descRaw: String,
    val source: String?,
    val iconHint: Int?,
    val type: Int,
    val price: Int?,
    val sellboxPrice: Int?,
    val iconFrameKey: String,
    val categoryLabel: String,
    val group: ItemGroup,
)
