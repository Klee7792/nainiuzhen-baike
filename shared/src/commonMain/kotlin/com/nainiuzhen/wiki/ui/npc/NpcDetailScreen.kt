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
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            header()
            Spacer(Modifier.size(12.dp))
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
        horizontalArrangement = Arrangement.spacedBy(16.dp),
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
            Text(
                text = "好感 max：${npc.maxStar} 心",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}

/** 弹窗可滚动主体：身份故事（自适应高度）+ 分割线 + 最爱 / 喜欢 / 讨厌。 */
@Composable
private fun NpcDetailBody(
    npc: NpcInfo,
    onItemClick: (ItemInfo) -> Unit,
) {
    val data = LocalDataRepository.current

    // 背景故事：按内容自适应，不再固定高度与虚化。
    RichText(
        raw = npc.descRaw,
        modifier = Modifier.fillMaxWidth(),
    )

    // 身份故事区 与 物品偏好区 之间的 80% 分割线。
    Box(
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .padding(vertical = 12.dp),
    ) {
        HorizontalDivider(modifier = Modifier.fillMaxWidth())
    }

    // 最爱 / 喜欢 / 讨厌
    FavorSection(title = "最爱", color = FAVOR_COLORS["最爱"]!!, ids = npc.bestFavorItems, data = data, onItemClick = onItemClick)
    FavorSection(title = "喜欢", color = FAVOR_COLORS["喜欢"]!!, ids = npc.likeItems, data = data, onItemClick = onItemClick)
    FavorSection(title = "讨厌", color = FAVOR_COLORS["讨厌"]!!, ids = npc.hateItems, data = data, onItemClick = onItemClick)
}

/** 物品列表分区（最爱 / 喜欢 / 讨厌）：标题恒显并区分色；内容为空时保留最小高度。 */
@Composable
private fun FavorSection(
    title: String,
    color: Color,
    ids: List<Int>,
    data: DataRepository,
    onItemClick: (ItemInfo) -> Unit,
) {
    Spacer(Modifier.size(8.dp))
    SmallTitle(text = title, textColor = color)
    Card(modifier = Modifier.fillMaxWidth()) {
        if (ids.isNotEmpty()) {
            ItemCardRow(
                items = ids.map { data.itemById(it) },
                modifier = Modifier.padding(12.dp),
                onItemClick = onItemClick,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "（无）",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
    }
}

/** 最爱 / 喜欢 / 讨厌区分色。 */
private val FAVOR_COLORS = mapOf(
    "最爱" to Color(0xFFE91E63),
    "喜欢" to Color(0xFF43A047),
    "讨厌" to Color(0xFF9E9E9E),
)
