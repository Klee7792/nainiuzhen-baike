package com.nainiuzhen.wiki.ui.items

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nainiuzhen.wiki.data.model.ItemInfo
import com.nainiuzhen.wiki.ui.components.AppSubPageScaffold
import com.nainiuzhen.wiki.ui.components.SpriteImage
import com.nainiuzhen.wiki.ui.components.searchFieldColors
import com.nainiuzhen.wiki.ui.nav.LocalDataRepository
import com.nainiuzhen.wiki.ui.nav.LocalNavigator
import com.nainiuzhen.wiki.utils.LocalAppSettings
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Filter
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/** 筛选胶囊间距（横向与纵向一致，视觉更整齐）。 */
private val FILTER_CHIP_SPACING = 8.dp

/** 筛选弹窗内容区最大高度（收敛 [OverlayDialog] 的 Infinity 约束，pit #1）。 */
private val FILTER_DIALOG_MAX_HEIGHT = 600.dp

/**
 * 物品大全列表：6 列正方形网格（方块=切片素材，下方一行 11sp 名称，超出可横向滚动）。
 * 顶栏搜索框 + 右上角筛选 icon（categoryLabel 多选，胶囊形态）即时过滤。
 * 点击某物品弹出 [ItemDetailScreen]。
 *
 * v6 变更（变更点 #30 / #31）：
 * - 改用 [AppSubPageScaffold]，顶栏带模糊（高斯 / 渐进跟随设置）、大标题左对齐、内容区模糊采样。
 * - 名称超出宽度改为横向滚动（不再省略）。
 * - 筛选弹窗置于脚手架内容内，确保稳定显示分类选项。
 * - 跟随「滚动到末尾震动」开关挂载 [scrollEndHaptic]。
 *
 * v7 变更（bug-v6「物品大全页」）：
 * - 筛选弹窗改为胶囊多选（[FilterChipDialog]）：[FlowRow] 自动换行，末尾「重置」红字清空勾选。
 * - 搜索框跟随顶栏模糊（[searchFieldColors]）：搜索框属于顶栏的 `bottomContent`，与顶栏同处
 *   一个模糊容器内，顶栏模糊已覆盖该区域；这里只把输入框自身背景改为半透明，让顶栏的模糊
 *   透出来。复用顶栏同一份 backdrop，**不再额外 capture 一层内容**，避免每帧多一次整屏图层记录。
 * - 「共 xx 个物品」计数从网格首项（卡片滚动即消失）改为顶栏 `bottomContent` 的固定行：
 *   居中、12sp，并整体上移 2dp 贴紧标题。
 */
@Composable
fun ItemListScreen() {
    val navigator = LocalNavigator.current
    val data = LocalDataRepository.current
    val appState = LocalAppSettings.current
    val scrollBehavior = MiuixScrollBehavior()
    var selected by remember { mutableStateOf<ItemInfo?>(null) }
    var query by remember { mutableStateOf("") }
    var showFilter by remember { mutableStateOf(false) }
    val categories = remember(data) { data.items.map { it.categoryLabel }.distinct().sorted() }
    var selectedCats by remember { mutableStateOf<Set<String>>(emptySet()) }

    val base = remember(data, selectedCats) { data.itemsByCategory(selectedCats) }
    val filtered = remember(base, query) {
        if (query.isBlank()) base else base.filter { it.name.contains(query, ignoreCase = true) }
    }

    AppSubPageScaffold(
        title = "物品大全",
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            IconButton(onClick = { navigator.pop() }) {
                Icon(
                    imageVector = MiuixIcons.Back,
                    contentDescription = "返回",
                    tint = MiuixTheme.colorScheme.onBackground,
                )
            }
        },
        actions = {
            IconButton(onClick = { showFilter = true }) {
                Icon(
                    imageVector = MiuixIcons.Filter,
                    contentDescription = "筛选",
                    tint = MiuixTheme.colorScheme.onBackground,
                )
            }
        },
        bottomContent = {
            ItemListBottomContent(
                count = filtered.size,
                query = query,
                onQueryChange = { query = it },
            )
        },
    ) { innerPadding ->
        val gap = 8.dp
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            state = rememberLazyGridState(),
            modifier = Modifier
                .fillMaxHeight()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .then(if (appState.scrollEndHaptic) Modifier.scrollEndHaptic() else Modifier),
            contentPadding = PaddingValues(
                start = 12.dp,
                top = innerPadding.calculateTopPadding(),
                end = 12.dp,
                bottom = 12.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(gap),
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            items(filtered, key = { it.id }) { item ->
                ItemGridCell(item = item, onClick = { selected = item })
            }
        }
        ItemDetailScreen(item = selected, onDismissRequest = { selected = null })
        FilterChipDialog(
            show = showFilter,
            onDismissRequest = { showFilter = false },
            title = "筛选类别",
            options = categories,
            selected = selectedCats,
            onSelectedChange = { selectedCats = it },
        )
    }
}

/**
 * 顶栏 `bottomContent`：上方「共 xx 个物品」计数（居中、12sp、整体上移 2dp），下方搜索框。
 *
 * 计数置于此处而非网格首项，保证卡片滚动时始终可见。
 * 搜索框配色统一走 [searchFieldColors]，与配方 / NPC 两个板块保持一致。
 */
@Composable
private fun ItemListBottomContent(
    count: Int,
    query: String,
    onQueryChange: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "共 $count 个物品",
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-2).dp)
                .padding(bottom = 4.dp),
        )
        TextField(
            value = query,
            onValueChange = onQueryChange,
            label = "搜索物品",
            colors = searchFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}

/**
 * 类别筛选弹窗（胶囊多选，v7）：每个类别一个小胶囊，横向依次排布，到行尾自动换行（[FlowRow]）；
 * 末尾追加「重置」红字胶囊，点击清空全部勾选。
 *
 * 物品类别数量较多（> 10），按 v6 规则使用胶囊形态（区别于配方 / NPC 两种「偏好行」形态）。
 *
 * 根容器 `heightIn(max = [FILTER_DIALOG_MAX_HEIGHT])` 收敛 [OverlayDialog] 的 `Infinity` 约束
 * （pit #1），嵌套的 `verticalScroll` 才能正常测量。
 *
 * @param show 是否显示。
 * @param onDismissRequest 关闭回调。
 * @param title 标题（如「筛选类别」）。
 * @param options 全部可选项（类别 / 类型标签）。
 * @param selected 当前已选集合（空 = 全部）。
 * @param onSelectedChange 选中集合变化回调（即时过滤）。
 */
@Composable
private fun FilterChipDialog(
    show: Boolean,
    onDismissRequest: () -> Unit,
    title: String,
    options: List<String>,
    selected: Set<String>,
    onSelectedChange: (Set<String>) -> Unit,
) {
    OverlayDialog(
        show = show,
        onDismissRequest = onDismissRequest,
        title = title,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = FILTER_DIALOG_MAX_HEIGHT)
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

/**
 * 单个筛选胶囊：未选中用次要容器底色 + 常规文字色，选中用主题色底 + 反色文字；
 * [errorText] 为 true 时文字用错误色（用于「重置」）。
 */
@Composable
private fun FilterChip(
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
        )
    }
}

/** 单个物品方块：正方形切片 + 名称（11sp，超出宽度横向滚动）。 */
@Composable
private fun ItemGridCell(item: ItemInfo, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SpriteImage(
            frameKey = item.iconFrameKey,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentScale = ContentScale.Fit,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(top = 2.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = item.name,
                fontSize = 11.sp,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center,
                color = MiuixTheme.colorScheme.onSurface,
            )
        }
    }
}
