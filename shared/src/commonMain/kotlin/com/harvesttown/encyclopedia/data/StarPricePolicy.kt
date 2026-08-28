package com.harvesttown.encyclopedia.data

import kotlin.math.roundToInt

/**
 * 星价判定策略（变更点 #13，数据驱动）。
 *
 * 仅白名单内 `categoryLabel` 的物品展示白(×1.2) / 金(×1.5) / 紫(×2.0) 三档星价；
 * 星价值 = round(price × 倍率)，使用 stdlib [kotlin.math.roundToInt]（pit #7，禁止自定义 Float 扩展）。
 *
 * [WHITELIST] 为设计文档 §7 的真实 `categoryLabel` 枚举（已在 `item_database.json` 全量核对），
 * 见设计文档「七、星价白名单最终结论」。
 */
object StarPricePolicy {
    /** 展示星价的类别白名单（真实 `categoryLabel` 枚举，base 名）。 */
    val WHITELIST: Set<String> = setOf(
        // 鲜花 / 山珍 / 药材
        "鲜花类", "山珍类", "药材类", "珍稀药材",
        // 鱼&虾&贝类
        "鱼类", "河鱼", "湖鱼", "海鱼", "珍稀鱼类", "虾蟹", "贝壳类",
        // 畜牧 / 加工 / 矿物 / 虫 / 怪物掉落
        "畜牧产品", "加工品", "矿物", "虫子", "怪物掉落",
    )

    /**
     * 计算物品的三档星级倍率信息。
     *
     * @param price 物品售价；为 null 时返回 null（仅展示普通售价，无星价）。
     * @param categoryLabel 物品类别标签；不为 null 且不在 [WHITELIST] 时返回 null（仅展示普通售价）。
     * @return 三档 [StarTier]（白 / 金 / 紫），或 null 表示「无星价类型，仅显示售价卡」。
     */
    fun tiers(price: Int?, categoryLabel: String? = null): List<StarTier>? {
        if (price == null) return null
        if (categoryLabel != null && categoryLabel !in WHITELIST) return null
        return listOf(
            StarTier(imageLevel = 1, multiplier = 1.2, label = "白星"),
            StarTier(imageLevel = 2, multiplier = 1.5, label = "金星"),
            StarTier(imageLevel = 3, multiplier = 2.0, label = "紫星"),
        )
    }
}

/**
 * 单档星级倍率信息。
 *
 * @param imageLevel 星级图级别：1=白(lv_2) / 2=金(lv_3) / 3=紫(lv_4)，对应 [com.harvesttown.encyclopedia.ui.components.StarImage]。
 * @param multiplier 售价倍率（白 1.2 / 金 1.5 / 紫 2.0）。
 * @param label 中文档名（白星 / 金星 / 紫星）。
 */
data class StarTier(
    val imageLevel: Int,
    val multiplier: Double,
    val label: String,
) {
    /** 该档星价值 = round(price × multiplier)。 */
    fun value(price: Int): Int = (price * multiplier).roundToInt()
}
