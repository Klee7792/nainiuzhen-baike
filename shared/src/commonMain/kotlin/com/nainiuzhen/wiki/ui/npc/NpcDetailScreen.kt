package com.nainiuzhen.wiki.ui.npc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nainiuzhen.wiki.data.model.ItemInfo
import com.nainiuzhen.wiki.data.model.NpcInfo
import com.nainiuzhen.wiki.data.repository.DataRepository
import com.nainiuzhen.wiki.ui.adaptive.DetailPaneEmptyHint
import com.nainiuzhen.wiki.ui.components.DetailPaneScaffold
import com.nainiuzhen.wiki.ui.components.ItemCardRow
import com.nainiuzhen.wiki.ui.components.SpriteScaleContext
import com.nainiuzhen.wiki.ui.components.NpcPortraitImage
import com.nainiuzhen.wiki.ui.components.RichText
import com.nainiuzhen.wiki.ui.components.rememberDialogMaxHeight
import com.nainiuzhen.wiki.ui.items.ItemDetailScreen
import com.nainiuzhen.wiki.ui.nav.LocalDataRepository
import com.nainiuzhen.wiki.ui.nav.LocalNavigator
import com.nainiuzhen.wiki.ui.nav.Route
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.FavoritesFill
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * NPC 详情弹窗。
 *
 * v7 变更（变更点 #33 / #34 / #41）:
 * 1. 图示 & 名称区转换为固定顶栏，不随内容滚动；左侧图示放大到 120%（144.dp）。
 * 2. 背景故事区取消固定高度与底部虚化，改为按内容自适应高度。
 * 3. 最爱 / 喜欢 / 讨厌横向滚动时保持两侧淡化效果（[ItemCardRow] 内部处理）。
 * 4. 关闭按钮与日程按钮同宽，左右对称。
 * 5. 在身份故事区与物品偏好区之间加入 80% 宽度的分割线；按钮区与上方、顶栏与下方不加分割线。
 * 6. 弹窗整体不再固定高度，使用 [OverlayDialog] 自适应，但保留默认外边距、宽度、底部对齐与按钮区样式。
 */
@Composable
fun NpcDetailScreen(npc: NpcInfo?, onDismissRequest: () -> Unit) {
    var nestedItem by remember { mutableStateOf<ItemInfo?>(null) }

    NpcDetailDialog(
        show = npc != null,
        onDismissRequest = onDismissRequest,
        header = { npc?.let { NpcDetailHeader(npc = it) } },
        buttons = {
            if (npc != null) {
                NpcDetailButtons(npc = npc, onClose = onDismissRequest)
            }
        },
    ) {
        npc?.let { NpcDetailBody(npc = it, onItemClick = { nestedItem = it }) }
    }

    // 最爱 / 喜欢 / 讨厌卡片穿透出的物品详情（叠加在 NPC 对话框之上）
    ItemDetailScreen(item = nestedItem, onDismissRequest = { nestedItem = null })
}

/**
 * 右栏「NPC 详情」面板（大屏双栏：左侧列表 + 右侧详情）。
 *
 * 未选中（`npc == null`）时显示 [DetailPaneEmptyHint]；否则用 [DetailPaneScaffold] 承载
 * 固定顶栏（[NpcDetailHeader]）+ 可滚动正文（[NpcDetailBody]）+「日程 / 关闭」按钮行。
 * 结尾保留偏好卡片穿透出的物品详情弹窗（叠加在右栏面板之上）。
 *
 * @param npc 当前选中的 NPC；为 null 时右栏显示空态。
 * @param onClose 关闭 / 清空选中回调。
 * @param modifier 外层修饰。
 */
@Composable
fun NpcDetailPane(
    npc: NpcInfo?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var nestedItem by remember { mutableStateOf<ItemInfo?>(null) }
    if (npc == null) {
        DetailPaneEmptyHint(modifier = modifier)
    } else {
        DetailPaneScaffold(
            modifier = modifier,
            header = { NpcDetailHeader(npc = npc) },
            buttons = { NpcDetailButtons(npc = npc, onClose = onClose) },
        ) {
            NpcDetailBody(npc = npc, onItemClick = { nestedItem = it })
        }
    }

    // 最爱 / 喜欢 / 讨厌卡片穿透出的物品详情（叠加在右栏面板之上）
    ItemDetailScreen(item = nestedItem, onDismissRequest = { nestedItem = null })
}

/**
 * NPC 详情的按钮行：「日程」+「关闭」两个按钮各 `weight(1f)`，由 Row 均分剩余空间、严格等宽。
 * 弹窗版（[NpcDetailScreen]）与双栏 pane 版（[NpcDetailPane]）共用，避免两处定义漂移。
 *
 * 「日程」`enabled = npc.maxStar > 0`，点击后先 push 日程路由再 [onClose]；逻辑与改造前一致。
 *
 * @param npc 当前 NPC。
 * @param onClose 关闭 / 清空选中回调。
 */
@Composable
private fun RowScope.NpcDetailButtons(npc: NpcInfo, onClose: () -> Unit) {
    val navigator = LocalNavigator.current
    // 两个按钮各 weight(1f)，由 Row 均分剩余空间，保证日程/关闭严格等宽
    // （weight 是 RowScope 成员扩展，在此 lambda 内无需 import androidx.compose.foundation.layout.weight）。
    TextButton(
        text = "日程",
        enabled = npc.maxStar > 0,
        onClick = {
            if (npc.maxStar > 0) {
                navigator.push(Route.NpcSchedule(npc.id))
                onClose()
            }
        },
        colors = ButtonDefaults.textButtonColorsPrimary(),
        modifier = Modifier.weight(1f),
    )
    TextButton(
        text = "关闭",
        onClick = onClose,
        modifier = Modifier.weight(1f),
    )
}

/**
 * 自定义 NPC 详情弹窗骨架：顶栏固定、中部可滚动、按钮置底。
 * 与 [BasicDetailDialog] 保持一致的外边距、宽度、底部对齐，但增加固定 header 插槽。
 *
 * H0 热修（横屏按钮消失）：
 * 1. 外层总高度由硬编码 640.dp 改为 [rememberDialogMaxHeight]（= min(640.dp, 窗口高 × 0.9)），
 *    横屏（可用高 ≈393dp）时不再溢出屏幕。
 * 2. 中部内容列由硬编码 `heightIn(max = 400.dp)` 改为 `weight(1f, fill = false)`：
 *    高度上限交给外层 Column 的剩余空间（- header - 按钮行），不再三重限高叠加。
 * 3. 按钮 Row 非加权、排在加权内容之后，Column 先测量非加权子项，故按钮始终保有自然高度，
 *    横屏下「日程」入口不再被压成 0 高。
 */
@Composable
private fun NpcDetailDialog(
    show: Boolean,
    onDismissRequest: () -> Unit,
    header: @Composable () -> Unit,
    buttons: @Composable RowScope.() -> Unit,
    content: @Composable () -> Unit,
) {
    val resolvedMaxHeight = rememberDialogMaxHeight(640.dp)
    // 强制底部贴合：miuix 在大屏（宽≥840dp 且 高≥480dp）会改为居中，导致横屏底部留白过大。
    // 显式传 largeScreen = false，使横屏与竖屏观感一致（均贴底）。
    OverlayDialog(
        show = show,
        onDismissRequest = onDismissRequest,
        largeScreen = false,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = resolvedMaxHeight)
                .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            header()
            Spacer(Modifier.size(2.dp))
            // 中部可滚动内容：weight 使高度上限 = 外层剩余空间，既收敛 OverlayDialog 的
            // Infinity 约束（避免 verticalScroll 崩溃），又保证按钮行必有位置。
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                content()
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                buttons()
            }
        }
    }
}

/**
 * 固定顶栏（高 144.dp），左右分栏：
 * - 左侧：140.dp 正方形容器，NPC 立绘填满高度、xy 居中、两侧裁剪（[ContentScale.Crop]）。
 * - 右侧：名称区（四层：名字 + 住址 + 生日 + 好感），层级划分为「名字」与「信息组」两级，
 *   名字调大 2 号（18→20.sp）并左对齐，信息组紧凑排布。
 *
 * 间距（相对 dialog 外框）：左图左缘 = OverlayDialog insideMargin 24 + 外层 Column 水平 12 = 36.dp；
 * 名称区右缘同理 36.dp；左图与名称区间距 = Row spacedBy 12.dp。
 */
@Composable
private fun NpcDetailHeader(npc: NpcInfo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(144.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 左侧：140.dp 正方形，立绘填满高度、xy 居中、两侧裁剪。
        Box(
            modifier = Modifier
                .size(140.dp)
                .background(
                    color = MiuixTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(16.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            NpcPortraitImage(
                npcId = npc.id,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        // 右侧：名称区（四层，层级划分为「名字」+「信息组」）。
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center,
        ) {
            // 第一级：名字（调大 2 号、左对齐）
            Text(
                text = npc.name,
                style = MiuixTheme.textStyles.title4.copy(fontSize = 20.sp),
                textAlign = TextAlign.Start,
                color = MiuixTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.size(8.dp))
            // 第二级：信息组（住址 / 生日 / 好感），组内紧凑。
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "住址：${npc.address}",
                    style = MiuixTheme.textStyles.body2,
                    textAlign = TextAlign.Start,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                Text(
                    text = "生日：${npc.birthday}",
                    style = MiuixTheme.textStyles.body2,
                    textAlign = TextAlign.Start,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                // 好感 max：以「好感：N」+ 红色矢量心图标呈现（替代橙色双心与 emoji）。
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "好感：${npc.maxStar}",
                        style = MiuixTheme.textStyles.body2,
                        textAlign = TextAlign.Start,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                    Icon(
                        imageVector = MiuixIcons.FavoritesFill,
                        contentDescription = null,
                        tint = Color(0xFFE53935),
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

/** 弹窗可滚动主体：最爱 / 喜欢 / 讨厌（无内容分区省略）→ 分割线 → 人物介绍（移到底部）。 */
@Composable
private fun NpcDetailBody(
    npc: NpcInfo,
    onItemClick: (ItemInfo) -> Unit,
) {
    val data = LocalDataRepository.current

    // 最爱 / 喜欢 / 讨厌（分区内部已过滤 id=0 与不存在物品；分区无内容则整体省略）
    FavorSection(title = "最爱", color = FAVOR_COLORS["最爱"]!!, ids = npc.bestFavorItems, data = data, onItemClick = onItemClick)
    FavorSection(title = "喜欢", color = FAVOR_COLORS["喜欢"]!!, ids = npc.likeItems, data = data, onItemClick = onItemClick)
    FavorSection(title = "讨厌", color = FAVOR_COLORS["讨厌"]!!, ids = npc.hateItems, data = data, onItemClick = onItemClick)

    // 人物介绍移到最爱/喜欢/讨厌下面：仅当至少一个分区有内容时，上方加 80% 分割线
    if (hasAnyFavor(npc)) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(vertical = 12.dp),
        ) {
            HorizontalDivider(modifier = Modifier.fillMaxWidth())
        }
    }
    // 人物介绍（背景故事）：按内容自适应，不再固定高度与虚化。
    RichText(
        raw = npc.descRaw,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** 任一偏好分区是否存在有效物品（id≠0 且物品存在）。 */
private fun hasAnyFavor(npc: NpcInfo): Boolean =
    npc.bestFavorItems.any { it != 0 } || npc.likeItems.any { it != 0 } || npc.hateItems.any { it != 0 }

/** 物品列表分区（最爱 / 喜欢 / 讨厌）：标题区分色；分区无有效物品时整体省略不显示。 */
@Composable
private fun FavorSection(
    title: String,
    color: Color,
    ids: List<Int>,
    data: DataRepository,
    onItemClick: (ItemInfo) -> Unit,
) {
    // 过滤：物品 ID=0（配置文件占位）或不存在的物品
    val items = ids.mapNotNull { id -> if (id != 0) data.itemById(id) else null }
    if (items.isEmpty()) return
    Spacer(Modifier.size(4.dp))
    Column {
        SmallTitle(text = title, textColor = color)
        // 标题下加同色下划线
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(top = 2.dp),
            thickness = 1.dp,
            color = color.copy(alpha = 0.5f),
        )
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        ItemCardRow(
            items = items,
            modifier = Modifier.padding(12.dp),
            onItemClick = onItemClick,
            scaleContext = SpriteScaleContext.DialogFavHate,
        )
    }
}

/** 最爱 / 喜欢 / 讨厌区分色。 */
private val FAVOR_COLORS = mapOf(
    "最爱" to Color(0xFFE91E63),
    "喜欢" to Color(0xFF43A047),
    "讨厌" to Color(0xFF9E9E9E),
)
