package com.nainiuzhen.wiki.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 收缩顶栏：基于 miuix [TopAppBar]，随内容滚动收缩（需配合 [ScrollBehavior] 注入 nestedScroll）。
 *
 * @param title 标题。
 * @param scrollBehavior 滚动行为（用于收缩）。
 * @param navigationIcon 左侧返回图标。
 * @param actions 右侧操作图标。
 * @param bottomContent 标题下方的扩展内容（如搜索框），收缩时一并收起。
 */
@Composable
fun CollapsibleTopBar(
    title: String,
    scrollBehavior: ScrollBehavior,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    bottomContent: @Composable () -> Unit = {},
) {
    TopAppBar(
        title = title,
        modifier = modifier,
        color = MiuixTheme.colorScheme.surface,
        scrollBehavior = scrollBehavior,
        navigationIcon = navigationIcon,
        actions = actions,
        bottomContent = bottomContent,
    )
}
