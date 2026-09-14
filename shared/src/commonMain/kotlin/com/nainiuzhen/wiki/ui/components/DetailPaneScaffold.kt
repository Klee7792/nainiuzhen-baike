package com.nainiuzhen.wiki.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 右栏详情骨架：把 [BasicDetailDialog] 的内部结构照搬到「非弹窗」场景（大屏列表-详情双栏的右栏）。
 *
 * 与 [BasicDetailDialog] 同构，同样是**三分区结构**：可选的固定 header + 可滚动内容区 + 非加权按钮行，
 * 三者按此顺序排布，保证按钮恒可见（H0 热修的核心结论，绝不能退化）。
 * - 内容列 `weight(1f, fill = false)`：内容短时按内容自适应收窄，长时吃掉剩余空间并滚动。
 * - 按钮行非加权、排在加权内容之后：Column 先测量非加权子项，按钮始终保有自然高度。
 *
 * 与 [BasicDetailDialog] 的唯一差别：**不限高**（`fillMaxSize`，不再
 * `heightIn(max = rememberDialogMaxHeight(...))`），因为右栏本就被外层容器限定了尺寸；
 * 同时不含 [top.yukonga.miuix.kmp.overlay.OverlayDialog]，直接填满所在容器。
 *
 * @param modifier 外层修饰（通常由调用方传入 `fillMaxSize`）。
 * @param header 可选固定顶栏（如 NPC 的图示 + 名称区）；为 null 时不渲染。
 * @param buttons 底部按钮行内容（在 [RowScope] 内布局），恒可见。
 * @param content 可滚动内容。
 */
@Composable
fun DetailPaneScaffold(
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
    buttons: @Composable RowScope.() -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (header != null) {
            header()
            Spacer(Modifier.height(2.dp))
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            content()
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            buttons()
        }
    }
}
