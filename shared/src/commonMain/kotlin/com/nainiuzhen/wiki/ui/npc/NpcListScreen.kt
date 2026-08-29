package com.nainiuzhen.wiki.ui.npc

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.nainiuzhen.wiki.data.model.NpcInfo
import com.nainiuzhen.wiki.ui.components.AppSubPageScaffold
import com.nainiuzhen.wiki.ui.components.FilterChipDialog
import com.nainiuzhen.wiki.ui.components.NpcPortraitImage
import com.nainiuzhen.wiki.ui.components.searchFieldColors
import com.nainiuzhen.wiki.ui.nav.LocalDataRepository
import com.nainiuzhen.wiki.ui.nav.LocalNavigator
import com.nainiuzhen.wiki.utils.LocalAppSettings
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Filter
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * NPC 资料列表：一行 3 列卡片网格（立绘保持长宽比填满卡片 + 名称，超出横向滚动）。
 * 顶栏搜索框 + 右上角筛选 icon（性别分组多选）即时过滤。点击弹出 [NpcDetailScreen] 详情。
 *
 * v7→v8 变更：
 * 1. 筛选弹窗改用共享胶囊 [FilterChipDialog]（与物品子页一致），替换原右对齐复选框行。
 * 2. 列表卡片立绘恢复正常比例（移除 120% 放大；放大改到主页板块入口与详情 dialog 左侧图）。
 * 3. 搜索框改用共享配色 [searchFieldColors]，与顶栏模糊同步（模糊生效时半透明）。
 */
@Composable
fun NpcListScreen() {
    val navigator = LocalNavigator.current
    val data = LocalDataRepository.current
    val appState = LocalAppSettings.current
    val scrollBehavior = MiuixScrollBehavior()
    var selected by remember { mutableStateOf<NpcInfo?>(null) }
    var query by remember { mutableStateOf("") }
    var showFilter by remember { mutableStateOf(false) }
    val groups = remember(data) { data.npcs.map { npcGroupLabel(it.sex) }.distinct().sorted() }
    var selectedGroups by remember { mutableStateOf<Set<String>>(emptySet()) }

    val base = remember(data, selectedGroups) {
        if (selectedGroups.isEmpty()) {
            data.npcs
        } else {
            data.npcs.filter { npcGroupLabel(it.sex) in selectedGroups }
        }
    }
    val filtered = remember(base, query) {
        if (query.isBlank()) base else base.filter { it.name.contains(query, ignoreCase = true) }
    }

    AppSubPageScaffold(
        title = "NPC 资料",
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
                label = "搜索 NPC",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                colors = searchFieldColors(),
            )
        },
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            state = rememberLazyGridState(),
            modifier = Modifier
                .fillMaxHeight()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .then(if (appState.scrollEndHaptic) Modifier.scrollEndHaptic() else Modifier),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = 12.dp,
                start = 12.dp,
                end = 12.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(filtered, key = { it.id }) { npc ->
                NpcGridCell(npc = npc, onClick = { selected = npc })
            }
        }
        NpcDetailScreen(npc = selected, onDismissRequest = { selected = null })
        FilterChipDialog(
            show = showFilter,
            onDismissRequest = { showFilter = false },
            title = "筛选分组",
            options = groups,
            selected = selectedGroups,
            onSelectedChange = { selectedGroups = it },
        )
    }
}

/** NPC 性别分组标签（用于筛选）。 */
private fun npcGroupLabel(sex: Int): String = when (sex) {
    1 -> "男"
    2 -> "女"
    else -> "其他"
}

/** 单个 NPC 卡片：立绘（正常比例，居中裁剪）+ 名称（单行，超出横向滚动）。 */
@Composable
private fun NpcGridCell(npc: NpcInfo, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                NpcPortraitImage(
                    npcId = npc.id,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = npc.name,
                    style = MiuixTheme.textStyles.body2,
                    maxLines = 1,
                    softWrap = false,
                    textAlign = TextAlign.Center,
                    color = MiuixTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
