package com.harvesttown.encyclopedia.ui.npc

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
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
import com.harvesttown.encyclopedia.ui.nav.LocalDataRepository
import com.harvesttown.encyclopedia.ui.nav.LocalNavigator
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
 * NPC 日程子页（完整路由 [com.harvesttown.encyclopedia.ui.nav.Route.NpcSchedule]）。
 * 按 星期 / 季节 / 天气 单维筛选，展示该 NPC 的日程（名称、开始时间、起止场景、适用维度）。
 */
@Composable
fun NpcScheduleScreen(npcId: Int) {
    val navigator = LocalNavigator.current
    val data = LocalDataRepository.current
    val scrollBehavior = MiuixScrollBehavior()
    val all = remember(npcId) { data.npcSchedules(npcId) }
    var week by remember { mutableStateOf<Int?>(null) }
    var season by remember { mutableStateOf<Int?>(null) }
    var weather by remember { mutableStateOf<Int?>(null) }

    val filtered = all.filter { s ->
        (week == null || week in s.week) &&
            (season == null || season in s.season) &&
            (weather == null || weather in s.weather)
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "日程",
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
            item(key = "filters") {
                Column(
                    modifier = Modifier
                        .background(MiuixTheme.colorScheme.surface)
                        .padding(horizontal = 12.dp),
                ) {
                    ScheduleFilterRow(label = "星期", options = (1..7).toList(), selected = week) {
                        week = if (week == it) null else it
                    }
                    ScheduleFilterRow(label = "季节", options = (1..4).toList(), selected = season) {
                        season = if (season == it) null else it
                    }
                    ScheduleFilterRow(label = "天气", options = (1..5).toList(), selected = weather) {
                        weather = if (weather == it) null else it
                    }
                }
            }
            items(filtered, key = { it.id }) { s ->
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = s.name,
                            style = MiuixTheme.textStyles.title4,
                            color = MiuixTheme.colorScheme.onBackground,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "时间：${s.startTimeText}",
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                        Text(
                            text = "路线：${s.startPoint.sceneName} → ${s.endPoint.sceneName}",
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                        Text(
                            text = "适用：周${s.week.joinToString("/")} · 季${s.season.joinToString("/")} · 天${s.weather.joinToString("/")}",
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
            }
        }
    }
}

/** 单维筛选行：标签 + 可单选的 chip（再次点击取消）。 */
@Composable
private fun ScheduleFilterRow(
    label: String,
    options: List<Int>,
    selected: Int?,
    onSelect: (Int?) -> Unit,
) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(vertical = 4.dp),
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(text = "全部", selected = selected == null) { onSelect(null) }
            }
            items(options) { opt ->
                FilterChip(text = opt.toString(), selected = selected == opt) { onSelect(opt) }
            }
        }
    }
}

/** 筛选 chip（选中高亮）。 */
@Composable
private fun FilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val backgroundColor =
        if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.surfaceContainer
    val contentColor =
        if (selected) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.onSurface
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = contentColor, fontSize = MiuixTheme.textStyles.body2.fontSize)
    }
}
