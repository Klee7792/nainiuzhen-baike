package com.nainiuzhen.wiki.data.model

/**
 * NPC 模型，由 `npc_database.json` 解析而来。
 *
 * @param id NPC id。
 * @param name 名称。
 * @param descRaw 人物简介（富文本，含 `\n`）。
 * @param address 住址。
 * @param birthday 生日。
 * @param sex 性别。
 * @param loveItemId 该 NPC 自己的「好感度」所对应的 items id。
 *     在游戏数据中好感度本身就是一个 items 的 id；给该 NPC 送礼后增长的正是它。
 *     它**不是**该 NPC 喜欢的物品 id（喜欢 / 讨厌 / 最爱分别由 [likeItems] / [hateItems] / [bestFavorItems] 表示）。
 * @param maxStar 好感上限（心数）；为 0 时日程按钮禁用。
 * @param likeItems 喜欢物品 id 列表。
 * @param hateItems 讨厌物品 id 列表。
 * @param bestFavorItems 最爱物品 id 列表。
 */
data class NpcInfo(
    val id: Int,
    val name: String,
    val descRaw: String,
    val address: String,
    val birthday: String,
    val sex: Int,
    val loveItemId: Int,
    val maxStar: Int,
    val likeItems: List<Int>,
    val hateItems: List<Int>,
    val bestFavorItems: List<Int>,
)
