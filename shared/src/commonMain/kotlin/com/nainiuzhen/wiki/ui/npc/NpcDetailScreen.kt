package com.nainiuzhen.wiki.ui.npc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.dp
import com.nainiuzhen.wiki.data.model.ItemInfo
import com.nainiuzhen.wiki.data.model.NpcInfo
import com.nainiuzhen.wiki.data.repository.DataRepository
import com.nainiuzhen.wiki.ui.components.ItemCardRow
import com.nainiuzhen.wiki.ui.components.NpcPortraitImage
import com.nainiuzhen.wiki.ui.components.RichText
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
    val navigator = LocalNavigator.current

    NpcDetailDialog(
        show = npc != null,
        onDismissRequest = onDismissRequest,
        header = { npc?.let { NpcDetailHeader(npc = it) } },
        buttons = {
            if (npc != null) {
                TextButton(
                    text = "日程",
                    enabled = npc.maxStar > 0,
                    onClick = {
                        if (npc.maxStar > 0) {
                            navigator.push(Route.NpcSchedule(npc.id))
                            onDismissRequest()
                        }
                    },
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    modifier = Modifier.fillMaxWidth(0.49f),
                )
                TextButton(
                    text = "关闭",
                    onClick = onDismissRequest,
                    modifier = Modifier.fillMaxWidth(0.49f),
                )
            }
        },
    ) {
        npc?.let { NpcDetailBody(npc = it, onItemClick = { nestedItem = it }) }
    }

    // 最爱 / 喜欢 / 讨厌卡片穿透出的物品详情（叠加在 NPC 对话框之上）
    ItemDetailScreen(item = nestedItem, onDismissRequest = { nestedItem = null })
}

/**
 * 自定义 NPC 详情弹窗骨架：顶栏固定、中部可滚动、按钮置底。
 * 与 [BasicDetailDialog] 保持一致的外边距、宽度、底部对齐，但增加固定 header 插槽。
 */
@Composable
private fun NpcDetailDialog(
    show: Boolean,
    onDismissRequest: () -> Unit,
    header: @Composable () -> Unit,
    buttons: @Composable RowScope.() -> Unit,
    content: @Composable () -> Unit,
) {
    OverlayDialog(
        show = show,
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            header()
            Spacer(Modifier.size(4.dp))
            // 中部可滚动内容：有限高度收敛 OverlayDialog 的 Infinity 约束，避免 verticalScroll 崩溃。
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                content()
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                buttons()
            }
        }
    }
}

/** 固定顶栏：NPC 立绘（放大 120%）+ 右侧名称 / 住址 / 生日 / 最高好感。 */
@Composable
private fun NpcDetailHeader(npc: NpcInfo) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NpcPortraitImage(
            npcId = npc.id,
            modifier = Modifier.height(144.dp),
            contentScale = ContentScale.Fit,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = npc.name,
                style = MiuixTheme.textStyles.title4,
                color = MiuixTheme.colorScheme.onBackground,
            )
            Text(
                text = "住址：${npc.address}",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Text(
                text = "生日：${npc.birthday}",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            // 好感 max：[max 图标] N 心 [空格] [max 图标]
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    imageVector = MiuixIcons.FavoritesFill,
                    contentDescription = null,
                    tint = Color(0xFFFFB300),
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = "${npc.maxStar} 心",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                Icon(
                    imageVector = MiuixIcons.FavoritesFill,
                    contentDescription = null,
                    tint = Color(0xFFFFB300),
                    modifier = Modifier.size(14.dp),
                )
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
    Spacer(Modifier.size(8.dp))
    Column {
        SmallTitle(text = title, textColor = color)
        // 标题下加同色下划线
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
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
        )
    }
}

/** 最爱 / 喜欢 / 讨厌区分色。 */
private val FAVOR_COLORS = mapOf(
    "最爱" to Color(0xFFE91E63),
    "喜欢" to Color(0xFF43A047),
    "讨厌" to Color(0xFF9E9E9E),
)
