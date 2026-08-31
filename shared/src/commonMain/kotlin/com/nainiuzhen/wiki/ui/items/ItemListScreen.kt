package com.nainiuzhen.wiki.ui.items

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import com.nainiuzhen.wiki.data.model.ItemInfo
import com.nainiuzhen.wiki.data.model.searchText
import com.nainiuzhen.wiki.ui.components.AppSubPageScaffold
import com.nainiuzhen.wiki.ui.components.FilterChipDialog
import com.nainiuzhen.wiki.ui.components.SpriteImage
import com.nainiuzhen.wiki.ui.components.SpriteScaleContext
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
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

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
 * - 「共 xx 个物品」计数走 miuix 原生 `subtitle`：展开时作为大标题第二行，且 `largeTitleCentered=true`
 *   使标题与计数整体水平居中（契合「Y 轴居中、左右对称」）；收起时由 miuix 原生 smallSubtitle 居中，
 *   与展开态保持一致、滚动无横向跳变。不再进 `bottomContent`。
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
        if (query.isBlank()) base else base.filter { it.searchText.contains(query, ignoreCase = true) }
    }

    AppSubPageScaffold(
        title = "物品大全",
        largeTitleCentered = true,
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
        subtitle = "共 ${filtered.size} 个物品",
        bottomContent = {
            ItemListBottomContent(
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
                .overScrollVertical()
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
 * 顶栏 `bottomContent`：仅搜索框。
 *
 * 「共 xx 个物品」计数已改为标题正下方的独立 overlay（见 [AppTopAppBar]），
 * 此处只保留搜索框，配色统一走 [searchFieldColors]，与配方 / NPC 两个板块保持一致。
 */
@Composable
private fun ItemListBottomContent(
    query: String,
    onQueryChange: (String) -> Unit,
) {
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


/** 单个物品方块：图标区（透明，正方形严格 4× 原图、xy 居中、不裁圆角）+ 下半名称区（透明、蓝色满宽胶囊，文字超宽在胶囊内滚动）。整体为 v20 风格方形卡片，按压水波纹为方形。 */
@Composable
private fun ItemGridCell(item: ItemInfo, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 图标区：透明（跟随内容区灰底）；图片严格 4× 原图、xy 居中，整张方图不裁圆角（v20 风格）。
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center,
        ) {
            SpriteImage(
                frameKey = item.iconFrameKey,
                contentScale = ContentScale.Fit,
                filterQuality = FilterQuality.None,
                scaleContext = SpriteScaleContext.Card,
            )
        }
        // 名称区：透明（跟随内容区灰底）、无圆角；胶囊宽度=卡片宽（固定），文字超宽在胶囊内部滚动。
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MiuixTheme.colorScheme.primary,
                    shape = RoundedCornerShape(percent = 50),
                )
                .padding(top = 2.dp, start = 8.dp, end = 8.dp),
        ) {
            // 内层滚动视口：宽度=胶囊内宽，文字超宽只在这里滚动，胶囊本身不动。
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = item.name,
                    fontSize = 11.sp,
                    maxLines = 1,
                    softWrap = false,
                    textAlign = TextAlign.Center,
                    color = MiuixTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}
