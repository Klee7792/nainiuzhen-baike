package com.nainiuzhen.wiki.ui.adaptive

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/**
 * 取「让单卡宽度 >= [minCard] 的最大列数」。
 * GridCells.Fixed 会把可用宽度均分给 n 列，因此必然填满整行、不会产生两侧留白。
 *
 * @param availableWidth 网格内容的真实可用宽度（已扣除左右内边距）。
 * @param spacing 相邻两列之间的横向间距。
 * @param minCard 单卡期望的最小宽度（列数取满足该下限的最大值）。
 * @return 至少为 1 的列数。
 */
fun adaptiveGridColumns(availableWidth: Dp, spacing: Dp, minCard: Dp): Int =
    ((availableWidth + spacing) / (minCard + spacing)).toInt().coerceAtLeast(1)

/**
 * 用 BoxWithConstraints 读取真实可用宽度，算出列数后交给 [grid] 建 LazyVerticalGrid。
 * 这样「卡片 min 尺寸」只存在于参数里，卡片自身保持 fillMaxWidth()，只有一处真相源。
 *
 * @param hPadding 网格左右各自的水平内边距（用于从可用宽度中扣除）。
 * @param spacing 相邻两列之间的横向间距。
 * @param minCard 单卡期望的最小宽度。
 * @param modifier 修饰符，附加到内部 BoxWithConstraints 上。
 * @param grid 构建网格的内容 lambda，入参为计算出的列数。
 */
@Composable
fun AdaptiveIconGrid(
    hPadding: Dp,
    spacing: Dp,
    minCard: Dp,
    modifier: Modifier = Modifier,
    grid: @Composable (columns: Int) -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        grid(adaptiveGridColumns(maxWidth - hPadding * 2, spacing, minCard))
    }
}
