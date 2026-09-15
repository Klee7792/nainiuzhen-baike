package com.nainiuzhen.wiki.utils

/**
 * 通用小数格式化（跨平台安全）：
 *
 * commonMain 不能用 JVM 专属的 `"%.Nf".format(x)`（Kotlin/Native 无此 API，2026-09-15 iOS 化排查），
 * 本实现走纯 Kotlin 整数运算，**且不受系统 Locale 影响**（旧 JVM 实现在某些区域设置下
 * 会输出逗号小数点）。仅支持非负数（本项目场景：倍率 / 字号 / 秒数，均为正）。
 *
 * @param decimals 小数位数（0~3）；超出按 0 处理。
 */
fun formatDecimal(value: Double, decimals: Int): String {
    val factor = when (decimals) {
        1 -> 10L
        2 -> 100L
        3 -> 1000L
        else -> 1L
    }
    // 四舍五入到目标小数位（仅非负数场景，+0.5 截断即正确）。
    val rounded = (value * factor + 0.5).toLong()
    val whole = rounded / factor
    return if (decimals == 0) {
        whole.toString()
    } else {
        val frac = (rounded % factor).toString().padStart(decimals, '0')
        "$whole.$frac"
    }
}
