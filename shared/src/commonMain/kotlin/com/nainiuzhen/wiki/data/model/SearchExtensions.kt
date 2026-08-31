package com.nainiuzhen.wiki.data.model

import com.nainiuzhen.wiki.richtext.RichTextParser

/**
 * 搜索增强：把各模型的「名称 / 详情 / 来源 / 其他文字字段」拼成一条可搜文本，
 * 使列表页搜索框不只匹配名称，还能命中 dialog 内的文字信息。
 */

/** 富文本原始串（含 `/#颜色#…/#`、`\n` 标记）→ 纯文本。空串返回空。 */
fun String?.toPlainText(): String {
    if (this.isNullOrEmpty()) return ""
    val sb = StringBuilder()
    for (span in RichTextParser.parse(this)) {
        when (span) {
            is TextSpan -> sb.append(span.text)
            is NewlineSpan -> sb.append(' ')
        }
    }
    return sb.toString()
}

/** 物品可搜文本：名称 + 来源 + 分类标签 + 详情（富文本转纯文本）。 */
val ItemInfo.searchText: String
    get() = buildString {
        append(name)
        append(' ')
        source?.let { append(it); append(' ') }
        append(categoryLabel)
        append(' ')
        append(descRaw.toPlainText())
    }

/** 配方可搜文本：名称 + 解锁/来源 + 类型标签 + 类别 + 描述（富文本转纯文本）。 */
val RecipeInfo.searchText: String
    get() = buildString {
        append(name)
        append(' ')
        deblockingDesc?.let { append(it); append(' ') }
        append(typeLabel)
        append(' ')
        append(category)
        append(' ')
        descRaw?.toPlainText()?.let { append(it) }
    }

/** NPC 可搜文本：名称 + 简介（富文本转纯文本）+ 住址 + 生日。 */
val NpcInfo.searchText: String
    get() = buildString {
        append(name)
        append(' ')
        append(descRaw.toPlainText())
        append(' ')
        append(address)
        append(' ')
        append(birthday)
    }
