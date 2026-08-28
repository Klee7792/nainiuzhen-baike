package com.harvesttown.encyclopedia.ui.npc

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.harvesttown.encyclopedia.data.model.NpcSchedule
import com.harvesttown.encyclopedia.ui.components.AppSubPageScaffold
import com.harvesttown.encyclopedia.ui.nav.LocalDataRepository
import com.harvesttown.encyclopedia.ui.nav.LocalNavigator
import com.harvesttown.encyclopedia.utils.LocalAppSettings
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * NPC 日程子页（完整路由 [com.harvesttown.encyclopedia.ui.nav.Route.NpcSchedule]）。
 *
 * 筛选区（单选、必选一项，默认 周一 / 晴天 / 春 / 未婚，无「全部」）：
 * 星期(周一到周天) / 天气(晴雨雪台风节日) / 季节(春夏秋冬) / 婚姻(未婚=0 / 已婚=1，按 [NpcSchedule.isAstar])。
 *
 * 日程区：左侧开始时间列 + 右侧名称（大字，仅显示 `|` 后的实际日程）+ 起止场景箭头（小字 `start → end`）。
 *
 * v6 变更（变更点 #30 / #36）：模糊顶栏；星期 7 个选项改为横向滚动（不再变竖向窄胶囊）；
 * 日程名称只显示竖线 `|` 后的实际内容。
 */
@Composable
fun NpcScheduleScreen(npcId: Int) {
    val navigator = LocalNavigator.current
    val data = LocalDataRepository.current
    val appState = LocalAppSettings.current
    val scrollBehavior = MiuixScrollBehavior()
    val all = remember(npcId) { data.npcSchedules(npcId) }
    val npcName = remember(npcId) { data.npcs.firstOrNull { it.id == npcId }?.name ?: "NPC" }

    // 单选、必选：默认值 周一(1) / 晴天(1) / 春(1) / 未婚(0)
    var week by remember { mutableStateOf(1) }
    var season by remember { mutableStateOf(1) }
    var weather by remember { mutableStateOf(1) }
    var marriage by remember { mutableStateOf(0) }

    val filtered = all.filter { s ->
        week in s.week &&
            season in s.season &&
            weather in s.weather &&
            s.isAstar == marriage
    }

    AppSubPageScaffold(
        title = "$npcName 日程",
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
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .then(if (appState.scrollEndHaptic) Modifier.scrollEndHaptic() else Modifier),
            contentPadding = PaddingValues(top = innerPadding.calculateTopPadding(), bottom = 12.dp),
        ) {
            item(key = "filters") {
                Column(
                    modifier = Modifier
                        .background(MiuixTheme.colorScheme.surface)
                        .padding(horizontal = 12.dp),
                ) {
                    RequiredFilterRow(
                        label = "星期",
                        options = (1..7).map { it to WEEK_LABELS[it - 1] },
                        selected = week,
                        onSelect = { week = it },
                        horizontalScrollEnabled = true,
                    )
                    RequiredFilterRow(
                        label = "天气",
                        options = (1..5).map { it to WEATHER_LABELS[it - 1] },
                        selected = weather,
                        onSelect = { weather = it },
                    )
                    RequiredFilterRow(
                        label = "季节",
                        options = (1..4).map { it to SEASON_LABELS[it - 1] },
                        selected = season,
                        onSelect = { season = it },
                    )
                    RequiredFilterRow(
                        label = "婚姻",
                        options = listOf(0 to "未婚", 1 to "已婚"),
                        selected = marriage,
                        onSelect = { marriage = it },
                    )
                }
            }
            if (filtered.isEmpty()) {
                item(key = "empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "当前筛选无匹配日程",
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
            }
            items(filtered, key = { it.id }) { s ->
                ScheduleEntry(schedule = s)
            }
        }
    }
}

/** 单条日程：左侧开始时间列 + 右侧名称（仅 `|` 后实际日程）与起止场景箭头，高度统一。 */
@Composable
private fun ScheduleEntry(schedule: NpcSchedule) {
    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = schedule.startTimeText,
                style = MiuixTheme.textStyles.title4,
                color = MiuixTheme.colorScheme.primary,
                modifier = Modifier.width(64.dp),
            )
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = schedule.name.parseScheduleName(),
                    style = MiuixTheme.textStyles.title4,
                    color = MiuixTheme.colorScheme.onBackground,
                )
                SpacerH(4.dp)
                Text(
                    text = "${schedule.startPoint.sceneName} → ${schedule.endPoint.sceneName}",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
    }
}

/** 解析日程名：仅保留竖线 `|` 后的实际日程；无竖线则原样返回。 */
private fun String.parseScheduleName(): String {
    val idx = indexOf('|')
    if (idx < 0) return this
    return substring(idx + 1).trim().ifBlank { this }
}

/** 必选单选筛选行：标签 + 可单选的 chip（点击已选项保持不变，无「全部」）。 */
@Composable
private fun RequiredFilterRow(
    label: String,
    options: List<Pair<Int, String>>,
    selected: Int,
    onSelect: (Int) -> Unit,
    horizontalScrollEnabled: Boolean = false,
) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(vertical = 4.dp),
        )
        val rowModifier = Modifier
            .padding(bottom = 4.dp)
            .then(if (horizontalScrollEnabled) Modifier.horizontalScroll(rememberScrollState()) else Modifier)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = rowModifier,
        ) {
            options.forEach { (value, text) ->
                RequiredChip(
                    text = text,
                    selected = selected == value,
                    onClick = { onSelect(value) },
                )
            }
        }
    }
}

/** 必选筛选 chip（选中高亮，不可取消）。 */
@Composable
private fun RequiredChip(text: String, selected: Boolean, onClick: () -> Unit) {
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

/** 竖直间距。 */
@Composable
private fun SpacerH(height: androidx.compose.ui.unit.Dp) {
    androidx.compose.foundation.layout.Spacer(Modifier.size(height))
}

/** 标签映射。 */
private val WEEK_LABELS = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周天")
private val WEATHER_LABELS = listOf("晴天", "雨天", "雪天", "台风", "节日")
private val SEASON_LABELS = listOf("春", "夏", "秋", "冬")
