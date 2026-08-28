package com.nainiuzhen.wiki.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.basic.BasicComponent

/**
 * 主页菜单卡片：基于 miuix [BasicComponent] 的一行。
 *
 * @param title 卡片标题（第一行）。
 * @param summary 卡片副标题（第二行）。
 * @param startContent 左侧正方形素材（物品图标 / NPC 立绘 / 占位）。
 * @param onClick 点击跳转回调。
 */
@Composable
fun MenuCard(
    title: String,
    summary: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    startContent: (@Composable () -> Unit)? = null,
) {
    BasicComponent(
        modifier = modifier,
        title = title,
        summary = summary,
        startAction = startContent,
        onClick = onClick,
    )
}
