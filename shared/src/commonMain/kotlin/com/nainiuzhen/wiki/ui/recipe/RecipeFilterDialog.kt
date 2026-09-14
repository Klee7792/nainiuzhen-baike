package com.nainiuzhen.wiki.ui.recipe

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.CheckboxLocation
import top.yukonga.miuix.kmp.preference.CheckboxPreference
import com.nainiuzhen.wiki.ui.components.rememberDialogMaxHeight

/**
 * 选项数量较少（< 10 项）时的筛选弹窗：每行「标题靠左、控件靠右」，与设置页「色彩模式」等
 * 偏好行的观感一致（v6 规则：分类 / 筛选项少于 10 项用靠右选项样式，多于 10 项改用胶囊 dialog）。
 *
 * 首行固定为「全部类型」（[allLabel]）：选中态即 [selected] 为空，点击可一键清空勾选，
 * 因此本弹窗不需要额外的「重置」按钮。
 *
 * 多选：[selected] 为空表示不过滤（全部），否则按集合内的标签过滤。
 *
 * 关键：根容器 [Modifier.heightIn] 把 [OverlayDialog] 的 `Infinity` 最大高度约束收敛为有限值
 * （pit #1），内部 `verticalScroll` 才能正常测量；同时高度仍按内容自适应（非固定高度）。
 * 上限经 [rememberDialogMaxHeight] 再压到「窗口高 × 0.9」以内，避免横屏时超出屏幕。
 *
 * @param show 是否显示。
 * @param options 全部可选项（如配方类型标签）。
 * @param selected 当前已选集合（空 = 全部）。
 * @param onSelectedChange 选中集合变化回调（即时过滤）。
 * @param onDismissRequest 关闭回调。
 * @param modifier 根容器修饰。
 * @param title 弹窗标题。
 * @param allLabel 「全部」行文案。
 * @param maxHeight 内容区最大高度（超过则滚动）。
 */
@Composable
fun FilterOptionsDialog(
    show: Boolean,
    options: List<String>,
    selected: Set<String>,
    onSelectedChange: (Set<String>) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "筛选类型",
    allLabel: String = "全部类型",
    maxHeight: Dp = 480.dp,
) {
    val resolvedMaxHeight = rememberDialogMaxHeight(maxHeight)
    // 强制底部贴合：miuix 在大屏（宽≥840dp 且 高≥480dp）会改为居中，导致横屏底部留白过大。
    // 显式传 largeScreen = false，使横屏与竖屏观感一致（均贴底）。
    OverlayDialog(
        show = show,
        onDismissRequest = onDismissRequest,
        title = title,
        largeScreen = false,
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(max = resolvedMaxHeight)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
        ) {
            FilterOptionRow(
                title = allLabel,
                checked = selected.isEmpty(),
                onCheckedChange = { onSelectedChange(emptySet()) },
            )
            options.forEach { option ->
                FilterOptionRow(
                    title = option,
                    checked = option in selected,
                    onCheckedChange = { checked ->
                        onSelectedChange(if (checked) selected + option else selected - option)
                    },
                )
            }
        }
    }
}

/**
 * 单行选项：标题居左、勾选框居右（[CheckboxLocation.End]），点击整行切换。
 *
 * @param title 选项文案。
 * @param checked 是否已勾选。
 * @param onCheckedChange 勾选态变化回调。
 */
@Composable
private fun FilterOptionRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    CheckboxPreference(
        title = title,
        checked = checked,
        onCheckedChange = onCheckedChange,
        checkboxLocation = CheckboxLocation.End,
    )
}
