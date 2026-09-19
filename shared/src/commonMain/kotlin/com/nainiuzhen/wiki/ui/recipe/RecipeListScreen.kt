package com.nainiuzhen.wiki.ui.recipe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nainiuzhen.wiki.data.model.RecipeInfo
import com.nainiuzhen.wiki.data.model.searchText
import com.nainiuzhen.wiki.ui.adaptive.AdaptiveIconGrid
import com.nainiuzhen.wiki.ui.components.AppSubPageScaffold
import com.nainiuzhen.wiki.ui.components.BlurredSearchField
import com.nainiuzhen.wiki.ui.components.CardImageBox
import com.nainiuzhen.wiki.ui.components.CardNameCapsule
import com.nainiuzhen.wiki.ui.components.FilterChipDialog
import com.nainiuzhen.wiki.ui.components.ProvideMarqueeCoordinator
import com.nainiuzhen.wiki.ui.components.SpriteImage
import com.nainiuzhen.wiki.ui.components.SpriteScaleContext
import com.nainiuzhen.wiki.ui.components.rememberCardPressModifiers
import com.nainiuzhen.wiki.ui.components.rememberImeDismisser
import com.nainiuzhen.wiki.ui.nav.LocalDataRepository
import com.nainiuzhen.wiki.ui.nav.LocalNavigator
import com.nainiuzhen.wiki.utils.CardSection
import com.nainiuzhen.wiki.utils.LocalAppSettings
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Filter
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * 配方查询列表：6 列正方形网格 + 名称底部区块（超出宽度横向滚动）。
 * 顶栏搜索框 + 右上角筛选 icon（typeLabel 多选）即时过滤。点击某配方弹出 [RecipeDetailScreen]。
 *
 * v6 变更（变更点 #30 / #31）：同 [com.nainiuzhen.wiki.ui.items.ItemListScreen] —— 模糊顶栏、
 * 名称横向滚动、筛选弹窗入脚手架、跟随「滚动到末尾震动」开关。
 *
 * v7 变更：
 * - 筛选弹窗改为 [FilterOptionsDialog]：配方类型仅 7 项（< 10），按 v6 规则使用「标题靠左、
 *   控件靠右」的偏好行样式（与设置页「色彩模式」一致），支持多选，首行「全部类型」可一键清空。
 * - 搜索框同步顶栏模糊：搜索框属于顶栏的 `bottomContent`，与顶栏同处一个模糊容器内，
 *   顶栏模糊已覆盖该区域，只需把输入框自身背景改为半透明让模糊透出即可。复用顶栏同一份
 *   backdrop（不再额外 capture 一层内容），避免每帧多一次整屏图层记录。配色统一走共享的
 *   [BlurredSearchField]（物品 / 配方 / NPC 三处一致），透明度常量见
 *   [com.nainiuzhen.wiki.ui.components.SEARCH_FIELD_BLUR_ALPHA]。
 */
@Composable
fun RecipeListScreen() {
    val navigator = LocalNavigator.current
    val data = LocalDataRepository.current
    val appState = LocalAppSettings.current
    val scrollBehavior = MiuixScrollBehavior()
    var selected by remember { mutableStateOf<RecipeInfo?>(null) }
    var query by remember { mutableStateOf("") }
    var showFilter by remember { mutableStateOf(false) }
    // 点卡片 / 点右上角筛选时先收起输入法（用户反馈：输入法不收、弹窗被挤到输入法上方）。
    val dismissIme = rememberImeDismisser()
    val types = remember(data) { data.recipes.map { it.typeLabel }.distinct().sorted() }
    var selectedTypes by remember { mutableStateOf<Set<String>>(emptySet()) }

    val base = remember(data, selectedTypes) { data.recipesByCategory(selectedTypes) }
    val filtered = remember(base, query) {
        if (query.isBlank()) base else base.filter { it.searchText.contains(query, ignoreCase = true) }
    }

    AppSubPageScaffold(
        title = "配方查询",
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
            IconButton(onClick = { dismissIme(); showFilter = true }) {
                Icon(
                    imageVector = MiuixIcons.Filter,
                    contentDescription = "筛选",
                    tint = MiuixTheme.colorScheme.onBackground,
                )
            }
        },
        subtitle = "共 ${filtered.size} 个配方",
        bottomContent = {
            BlurredSearchField(
                value = query,
                onValueChange = { query = it },
                label = "搜索配方",
            )
        },
    ) { innerPadding ->
        val gap = 8.dp
        AdaptiveIconGrid(
            hPadding = 12.dp,
            spacing = gap,
            minCard = 47.dp,
        ) { columns ->
            ProvideMarqueeCoordinator {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
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
                    items(filtered, key = { it.id }) { recipe ->
                        RecipeGridCell(recipe = recipe, onClick = { dismissIme(); selected = recipe })
                    }
                }
            }
        }
        RecipeDetailScreen(recipe = selected, onDismissRequest = { selected = null })
        FilterChipDialog(
            show = showFilter,
            onDismissRequest = { showFilter = false },
            title = "筛选类型",
            options = types,
            selected = selectedTypes,
            onSelectedChange = { selectedTypes = it },
        )
    }
}

/** 单个配方方块：素材区（[CardImageBox]）+ 名称区（[CardNameCapsule]）。背景 / 圆角 / 胶囊 / 按下阴影随「卡片外观设置」求值；素材仍严格 4× 原图、xy 居中。 */
@Composable
private fun RecipeGridCell(recipe: RecipeInfo, onClick: () -> Unit) {
    // card = 整卡可点击；image = 按下反馈（阴影 + 覆盖）只作用于素材区，文字区不受影响。
    val press = rememberCardPressModifiers(CardSection.Recipe, onClick)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(press.card),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 图标区：素材容器（背景色 / 圆角随「卡片外观设置」求值）。
        CardImageBox(
            section = CardSection.Recipe,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).then(press.image),
        ) {
            SpriteImage(
                frameKey = recipe.iconFrameKey,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                filterQuality = FilterQuality.None,
                scaleContext = SpriteScaleContext.Card,
            )
        }
        // 名称区：可选蓝胶囊 + 单行文字（超宽在胶囊内横向滚动）。
        // 字号随「素材缩放设置 → 卡片文字」倍率缩小（1.0 = 原始 11.sp）。
        val appState = LocalAppSettings.current
        CardNameCapsule(
            section = CardSection.Recipe,
            text = recipe.name,
            fontSize = appState.recipeCardTextSizeSp.sp,
        )
    }
}
