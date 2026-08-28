package com.nainiuzhen.wiki.data.model

/**
 * 物品分组。用于 P1 的 equip / headwear / element 分离与独立入口。
 * - [Items]：普通物品（含 element 类，后续拆分）。
 * - [Equip]：武器 & 农具（独立图集）。
 * - [Headwear]：服装穿戴（独立图集）。
 * - [Element]：带 element id 的物品（装饰预览来源）。
 * - [Recipe]：配方类（图纸 / 菜谱等）。
 */
enum class ItemGroup {
    Items,
    Equip,
    Headwear,
    Element,
    Recipe,
}
