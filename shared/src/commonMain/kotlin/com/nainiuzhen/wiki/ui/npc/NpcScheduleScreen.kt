package com.nainiuzhen.wiki.ui.npc

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import com.nainiuzhen.wiki.data.model.NpcSchedule
import com.nainiuzhen.wiki.ui.components.AppSubPageScaffold
import com.nainiuzhen.wiki.ui.nav.LocalDataRepository
import com.nainiuzhen.wiki.ui.nav.LocalNavigator
import com.nainiuzhen.wiki.utils.LocalAppSettings
import com.nainiuzhen.wiki.utils.LocalUpdateAppSettings
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.anim.DecelerateEasing
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * NPC 日程子页（完整路由 [com.nainiuzhen.wiki.ui.nav.Route.NpcSchedule]）。
 *
 * 筛选区（单选、必选一项，默认 周一 / 晴天 / 春 / 未婚，无「全部」）：
 * 星期(周一到周天) / 天气(晴雨雪台风节日) / 季节(春夏秋冬) / 婚姻(未婚=0 / 已婚=1，按 [NpcSchedule.isAstar])。
 *
 * 日程区：左侧开始时间列 + 右侧名称（大字，仅显示 `|` 后的实际日程）+ 起止场景箭头（小字 `start → end`）。
 *
 * 交互约定（对齐「3 板块搜索栏」）：
 * 1. 筛选区放入顶栏 `bottomContent`，与顶栏同处一个磨砂容器：模糊效果止于筛选区底边
 *    （等价于物品/配方/NPC 列表的搜索框底部边框），下方日程滚到顶栏下方即被模糊采样。
 * 2. 日程列表**不接入** `nestedScroll`，因此滚动只会让内容在筛选区下方区域移动，
 *    **不会**连带把展开态大标题收起为小标题；内容未超出屏幕则不滚动，超出才滚动。
 * 3. 不再对筛选区自身叠加 `textureBlur`（避免自引用采样），统一由顶栏磨砂层负责。
 */
@Composable
fun NpcScheduleScreen(npcId: Int) {
    val navigator = LocalNavigator.current
    val data = LocalDataRepository.current
    val appState = LocalAppSettings.current
    val updateAppState = LocalUpdateAppSettings.current
    val scrollBehavior = MiuixScrollBehavior()
    val all = remember(npcId) { data.npcSchedules(npcId) }
    val npcName = remember(npcId) { data.npcs.firstOrNull { it.id == npcId }?.name ?: "NPC" }

    // 单选、必选：默认值 周一(1) / 晴天(1) / 春(1) / 未婚(0)
    var week by remember { mutableStateOf(1) }
    var season by remember { mutableStateOf(1) }
    var weather by remember { mutableStateOf(1) }
    var marriage by remember { mutableStateOf(0) }

    // 筛选区展开态：初始值跟随「固定展开」设置 —— 固定（默认）展开进页、取消固定收起进页；
    // 页内右下角箭头可随时展开/收起（v41 用户需求）。
    var filterExpanded by remember { mutableStateOf(appState.scheduleFilterPinned) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (filterExpanded) 90f else -90f,
        label = "scheduleFilterChevron",
    )

    val filtered = all.filter { s ->
        week in s.week &&
            season in s.season &&
            weather in s.weather &&
            s.isAstar == marriage
    }

    AppSubPageScaffold(
        title = "$npcName 日程",
        scrollBehavior = scrollBehavior,
        // 日程页不接入 nestedScroll（保留展开大标题），因此渐进模糊的 alpha 无法随滚动
        // 从 0 升到 1；强制使用高斯模糊并保持常显，使筛选区底部始终有模糊背景。
        forceUniformBlur = true,
        navigationIcon = {
            IconButton(onClick = { navigator.pop() }) {
                Icon(
                    imageVector = MiuixIcons.Back,
                    contentDescription = "返回",
                    tint = MiuixTheme.colorScheme.onBackground,
                )
            }
        },
        // 筛选区：放入顶栏 bottomContent，与顶栏同处一个磨砂容器。
        // 模糊效果止于筛选区底边（底部细分割线标记该边界），下方日程滚到此处即被模糊。
        // 下方日程列表不接入 nestedScroll，滚动不会收起大标题。
        bottomContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(top = 4.dp, bottom = 8.dp)
                    // 4×1 ↔ 2×2 的高度补间：项目惯例 tween(300, DecelerateEasing(1.5f))。
                    // ⚠️ 必须用具名参数：`tween` 的第 2 个位置参数是 `delayMillis: Int`（不是 easing）。
                    .animateContentSize(tween(durationMillis = FILTER_ANIM_MS, easing = DecelerateEasing(1.5f))),
            ) {
                if (filterExpanded) {
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        // maxWidth 已是「扣掉左右各 12dp 内边距」后的可用宽度
                        val twoColumn = maxWidth >= FILTER_TWO_COLUMN_MIN_WIDTH
                        // 列间距用 FILTER_GROUP_GAP；行间距沿用 RequiredFilterRow 自带的 8dp 底部内边距，
                        // 故此处不加 verticalArrangement，保证 4×1 观感与改造前完全一致（改动最小）。
                        if (twoColumn) {
                            Column(Modifier.fillMaxWidth()) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(FILTER_GROUP_GAP),
                                    verticalAlignment = Alignment.Top,
                                ) {
                                    RequiredFilterRow("星期", (1..7).map { it to WEEK_LABELS[it - 1] }, week, { week = it })
                                    RequiredFilterRow("婚姻", listOf(0 to "未婚", 1 to "已婚"), marriage, { marriage = it })
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(FILTER_GROUP_GAP),
                                    verticalAlignment = Alignment.Top,
                                ) {
                                    RequiredFilterRow("天气", (1..5).map { it to WEATHER_LABELS[it - 1] }, weather, { weather = it })
                                    RequiredFilterRow("季节", (1..4).map { it to SEASON_LABELS[it - 1] }, season, { season = it })
                                }
                            }
                        } else {
                            // 4×1：顺序与原实现一致（星期→天气→季节→婚姻）；仅星期组保留横向滚动
                            Column(Modifier.fillMaxWidth()) {
                                RequiredFilterRow("星期", (1..7).map { it to WEEK_LABELS[it - 1] }, week, { week = it }, horizontalScrollEnabled = true)
                                RequiredFilterRow("天气", (1..5).map { it to WEATHER_LABELS[it - 1] }, weather, { weather = it })
                                RequiredFilterRow("季节", (1..4).map { it to SEASON_LABELS[it - 1] }, season, { season = it })
                                RequiredFilterRow("婚姻", listOf(0 to "未婚", 1 to "已婚"), marriage, { marriage = it })
                            }
                        }
                    }
                }
                // 右下角控制行：「固定」开关（持久化）+ 收起/展开箭头（Back 图标旋转成上下箭头）。
                // 固定（默认）= 每次进页展开；取消固定 = 每次进页收起；箭头随时手动切换本次展示。
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RequiredChip(
                        text = "固定",
                        selected = appState.scheduleFilterPinned,
                        onClick = {
                            val newPinned = !appState.scheduleFilterPinned
                            updateAppState(appState.copy(scheduleFilterPinned = newPinned))
                            // 点「固定」时若当前收起则立即展开，与「固定展开」语义一致
                            if (newPinned) filterExpanded = true
                        },
                    )
                    IconButton(onClick = { filterExpanded = !filterExpanded }) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = if (filterExpanded) "收起筛选" else "展开筛选",
                            tint = MiuixTheme.colorScheme.onBackground,
                            modifier = Modifier.rotate(chevronRotation),
                        )
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(top = 4.dp),
                    color = MiuixTheme.colorScheme.dividerLine,
                )
            }
        },
    ) { innerPadding ->
        // 日程区：充满剩余空间。不接入 nestedScroll，因此滚动只在本区域内发生，
        // 不会连带收起大标题。内容未超出屏幕高度则不滚动，超出才滚动。
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .overScrollVertical()
                .then(if (appState.scrollEndHaptic) Modifier.scrollEndHaptic() else Modifier),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = 12.dp,
            ),
        ) {
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
private val SEASON_LABELS = listOf("春天", "夏天", "秋天", "冬天")

/** 筛选区 4×1 ↔ 2×2 切换动画时长（ms）：对齐 miuix 弹窗遮罩惯例。 */
private const val FILTER_ANIM_MS = 300

/** 组间距：2×2 时两列之间的留白。 */
private val FILTER_GROUP_GAP = 12.dp

/**
 * 2×2 阈值 = 536dp（两行内容宽，见下）+ 8dp 余量。
 * 计算：胶囊宽 = 2字×14.sp + 24dp(水平内边距12+12) = 52dp；胶囊间距 8dp。
 * 行1 = 星期(7×52 + 6×8 = 412) + 12 + 婚姻(2×52 + 1×8 = 112) = 536dp
 * 行2 = 天气(5×52 + 4×8 = 292) + 12 + 季节(4×52 + 3×8 = 232) = 536dp
 * 两行总宽恒等（7+2 = 5+4 = 9）⇒ 右边缘天然对齐；列分割点不同（行1在第7个后、行2在第5个后）为配对方式决定。
 */
private val FILTER_TWO_COLUMN_MIN_WIDTH = 544.dp
