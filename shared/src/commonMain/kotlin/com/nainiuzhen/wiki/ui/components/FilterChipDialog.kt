package com.nainiuzhen.wiki.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 多选项筛选弹窗（胶囊形态，v8 抽离为共享组件）：
 * 物品 / 配方 / NPC 三个板块统一复用，保证筛选交互一致（bug-v7「同步使用物品子页那种小胶囊」）。
 *
 * 每个可选项一个小胶囊，横向依次排布、到行尾自动换行（[FlowRow]）；末尾追加「重置」红字胶囊，
 * 点击清空全部勾选（空集合 = 不过滤 = 全部）。
 *
 * 根容器 `heightIn(max = [rememberDialogMaxHeight]`([FILTER_DIALOG_MAX_HEIGHT])`)` 收敛 [OverlayDialog]
 * 的 `Infinity` 约束（pit #1），同时把上限压到「窗口高 × 0.9」以内，横屏时弹窗不再超出屏幕，
 * 嵌套的 `verticalScroll` 才能正常测量。
 */
@Composable
fun FilterChipDialog(
    show: Boolean,
    onDismissRequest: () -> Unit,
    title: String,
    options: List<String>,
    selected: Set<String>,
    onSelectedChange: (Set<String>) -> Unit,
) {
    val resolvedMaxHeight = rememberDialogMaxHeight(FILTER_DIALOG_MAX_HEIGHT)
    // 强制底部贴合：miuix 在大屏（宽≥840dp 且 高≥480dp）会改为居中，导致横屏底部留白过大。
    // 显式传 largeScreen = false，使横屏与竖屏观感一致（均贴底）。
    OverlayDialog(
        show = show,
        onDismissRequest = onDismissRequest,
        title = title,
        largeScreen = false,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = resolvedMaxHeight)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(FILTER_CHIP_SPACING),
                verticalArrangement = Arrangement.spacedBy(FILTER_CHIP_SPACING),
            ) {
                options.forEach { option ->
                    val isSelected = option in selected
                    FilterChip(
                        text = option,
                        selected = isSelected,
                        onClick = {
                            onSelectedChange(if (isSelected) selected - option else selected + option)
                        },
                    )
                }
                FilterChip(
                    text = "重置",
                    selected = false,
                    errorText = true,
                    onClick = { onSelectedChange(emptySet()) },
                )
            }
        }
    }
}

/** 筛选胶囊间距（横向与纵向一致）。 */
private val FILTER_CHIP_SPACING = 8.dp

/** 筛选弹窗内容区最大高度（收敛 [OverlayDialog] 的 Infinity 约束，pit #1）。 */
private val FILTER_DIALOG_MAX_HEIGHT = 600.dp

/**
 * 单个筛选胶囊：未选中用次要容器底色 + 常规文字色，选中用主题色底 + 反色文字；
 * [errorText] 为 true 时文字用错误色（用于「重置」）。
 */
@Composable
fun FilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    errorText: Boolean = false,
) {
    val backgroundColor = if (selected) {
        MiuixTheme.colorScheme.primary
    } else {
        MiuixTheme.colorScheme.secondaryContainer
    }
    val contentColor = when {
        errorText -> MiuixTheme.colorScheme.error
        selected -> MiuixTheme.colorScheme.onPrimary
        else -> MiuixTheme.colorScheme.onSurface
    }
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            color = contentColor,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}
