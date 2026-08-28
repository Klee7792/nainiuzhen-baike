package com.harvesttown.encyclopedia.ui.recipe

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harvesttown.encyclopedia.data.model.RecipeInfo
import com.harvesttown.encyclopedia.ui.components.FilterPopup
import com.harvesttown.encyclopedia.ui.components.SpriteImage
import com.harvesttown.encyclopedia.ui.nav.LocalDataRepository
import com.harvesttown.encyclopedia.ui.nav.LocalNavigator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Filter
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 配方查询列表：6 列正方形网格 + 名称底部区块（变更点 #14）。
 * 顶栏搜索框 + 右上角筛选 icon（typeLabel 多选）即时过滤。点击某配方弹出 [RecipeDetailScreen]。
 */
@Composable
fun RecipeListScreen() {
    val navigator = LocalNavigator.current
    val data = LocalDataRepository.current
    val scrollBehavior = MiuixScrollBehavior()
    var selected by remember { mutableStateOf<RecipeInfo?>(null) }
    var query by remember { mutableStateOf("") }
    var showFilter by remember { mutableStateOf(false) }
    val types = remember(data) { data.recipes.map { it.typeLabel }.distinct().sorted() }
    var selectedTypes by remember { mutableStateOf<Set<String>>(emptySet()) }

    val base = remember(data, selectedTypes) { data.recipesByCategory(selectedTypes) }
    val filtered = remember(base, query) {
        if (query.isBlank()) base else base.filter { it.name.contains(query, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "配方查询",
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
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        label = "搜索配方",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                },
            )
        },
    ) { innerPadding ->
        val gap = 8.dp
        LazyVerticalGrid(
            columns = GridCells.Fixed(6),
            state = rememberLazyGridState(),
            modifier = Modifier
                .fillMaxHeight()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                start = 12.dp,
                top = innerPadding.calculateTopPadding(),
                end = 12.dp,
                bottom = 12.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(gap),
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "header") {
                Text(
                    text = "共 ${filtered.size} 个配方",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MiuixTheme.colorScheme.surface)
                        .padding(vertical = 8.dp),
                )
            }
            items(filtered, key = { it.id }) { recipe ->
                RecipeGridCell(recipe = recipe, onClick = { selected = recipe })
            }
        }
        RecipeDetailScreen(recipe = selected, onDismissRequest = { selected = null })
    }
    FilterPopup(
        show = showFilter,
        onDismissRequest = { showFilter = false },
        title = "筛选类型",
        options = types,
        selected = selectedTypes,
        onSelectedChange = { selectedTypes = it },
    )
}

/** 单个配方方块：正方形切片 + 名称（11sp，居中，单行省略）。 */
@Composable
private fun RecipeGridCell(recipe: RecipeInfo, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SpriteImage(
            frameKey = recipe.iconFrameKey,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentScale = ContentScale.Fit,
        )
        Text(
            text = recipe.name,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MiuixTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
        )
    }
}
