package com.harvesttown.encyclopedia.ui.recipe

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.harvesttown.encyclopedia.data.model.RecipeInfo
import com.harvesttown.encyclopedia.ui.components.MenuCard
import com.harvesttown.encyclopedia.ui.components.SpriteImage
import com.harvesttown.encyclopedia.ui.nav.LocalDataRepository
import com.harvesttown.encyclopedia.ui.nav.LocalNavigator
import com.harvesttown.encyclopedia.ui.nav.Route
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 配方查询列表：按 typeLabel 多选筛选 + 列表。点击某配方弹出 [RecipeDetailScreen] 详情。
 */
@Composable
fun RecipeListScreen() {
    val navigator = LocalNavigator.current
    val data = LocalDataRepository.current
    val scrollBehavior = MiuixScrollBehavior()
    var selected by remember { mutableStateOf<RecipeInfo?>(null) }
    val types = remember(data) { data.recipes.map { it.typeLabel }.distinct().sorted() }
    var selectedTypes by remember { mutableStateOf<Set<String>>(emptySet()) }
    val filtered = remember(data, selectedTypes) { data.recipesByCategory(selectedTypes) }

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
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(top = innerPadding.calculateTopPadding(), bottom = 12.dp),
        ) {
            stickyHeader {
                Column(
                    modifier = Modifier
                        .background(MiuixTheme.colorScheme.surface)
                        .padding(horizontal = 12.dp),
                ) {
                    Text(
                        text = "共 ${filtered.size} 个配方",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 8.dp),
                    ) {
                        item {
                            CategoryChip(text = "全部", selected = selectedTypes.isEmpty()) {
                                selectedTypes = emptySet()
                            }
                        }
                        items(types) { type ->
                            CategoryChip(
                                text = type,
                                selected = type in selectedTypes,
                            ) {
                                selectedTypes =
                                    if (type in selectedTypes) selectedTypes - type else selectedTypes + type
                            }
                        }
                    }
                }
            }
            items(filtered, key = { it.id }) { recipe ->
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                ) {
                    MenuCard(
                        title = recipe.name,
                        summary = recipe.typeLabel,
                        startContent = {
                            SpriteImage(frameKey = recipe.iconFrameKey, modifier = Modifier.size(48.dp))
                        },
                        onClick = { selected = recipe },
                    )
                }
            }
        }
        RecipeDetailScreen(recipe = selected, onDismissRequest = { selected = null })
    }
}

/** 类型筛选 chip（选中高亮）。 */
@Composable
private fun CategoryChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val backgroundColor =
        if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.surfaceContainer
    val contentColor =
        if (selected) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.onSurface
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = contentColor, fontSize = MiuixTheme.textStyles.body2.fontSize)
    }
}
