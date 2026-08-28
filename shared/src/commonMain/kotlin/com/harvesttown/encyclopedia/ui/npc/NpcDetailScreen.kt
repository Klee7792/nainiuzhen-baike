package com.harvesttown.encyclopedia.ui.npc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.harvesttown.encyclopedia.data.model.ItemInfo
import com.harvesttown.encyclopedia.data.model.NpcInfo
import com.harvesttown.encyclopedia.data.repository.DataRepository
import com.harvesttown.encyclopedia.ui.components.BasicDetailDialog
import com.harvesttown.encyclopedia.ui.components.ExpandableRichText
import com.harvesttown.encyclopedia.ui.components.ItemCardRow
import com.harvesttown.encyclopedia.ui.components.NpcPortraitImage
import com.harvesttown.encyclopedia.ui.components.SpriteImage
import com.harvesttown.encyclopedia.ui.items.ItemDetailScreen
import com.harvesttown.encyclopedia.ui.nav.LocalDataRepository
import com.harvesttown.encyclopedia.ui.nav.LocalNavigator
import com.harvesttown.encyclopedia.ui.nav.Route
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * NPC 详情（以 [BasicDetailDialog] 承载，由列表页控制显隐；去标题栏、按钮置底，变更点 #21 / #22）。
 *
 * v5 布局（变更点 #15 ~ #19）：
 * 1. 立绘（按比例放大）与右侧 4 行信息（名称 / 住址 / 生日 / 好感 max）等高（固定 [Row] 高度）。
 * 2. 人物故事背景（内部滚动）。
 * 3. 最爱 / 喜欢 / 讨厌（收紧间距、左右边界与背景介绍对齐；为空也保留高度）。
 * 4. 按钮区：左侧「日程」（primary 强调色，[NpcInfo.maxStar] > 0 可进入）与右侧「关闭」等宽置底。
 *
 * 注意：已移除「好感度」行（对应物品无图标且不入背包，无意义，变更点 #16）。
 *
 * @param npc 当前选中的 NPC；为 null 时对话框不显示。
 * @param onDismissRequest 关闭回调。
 */
@Composable
fun NpcDetailScreen(npc: NpcInfo?, onDismissRequest: () -> Unit) {
    var nestedItem by remember { mutableStateOf<ItemInfo?>(null) }
    val navigator = LocalNavigator.current
    BasicDetailDialog(
        show = npc != null,
        onDismissRequest = onDismissRequest,
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

@Composable
private fun NpcDetailBody(
    npc: NpcInfo,
    onItemClick: (ItemInfo) -> Unit,
) {
    val data = LocalDataRepository.current

    // 1. 立绘 + 基础信息（等高：固定 Row 高度，立绘填满、右列居中）
    Row(
        modifier = Modifier.height(112.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NpcPortraitImage(
            npcId = npc.id,
            modifier = Modifier.fillMaxHeight(),
            contentScale = ContentScale.Fit,
        )
        Column(
            modifier = Modifier.fillMaxHeight(),
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

    // 2. 人物故事背景（内部滚动）
    Spacer(Modifier.size(12.dp))
    ExpandableRichText(raw = npc.descRaw, maxLines = 8)

    // 3. 最爱 / 喜欢 / 讨厌（收紧间距、左右边界与背景介绍对齐）
    FavorSection(title = "最爱", ids = npc.bestFavorItems, data = data, onItemClick = onItemClick)
    FavorSection(title = "喜欢", ids = npc.likeItems, data = data, onItemClick = onItemClick)
    FavorSection(title = "讨厌", ids = npc.hateItems, data = data, onItemClick = onItemClick)
}

/** 物品列表分区（最爱 / 喜欢 / 讨厌）：标题恒显；内容为空时保留最小高度，使对话框大小一致。 */
@Composable
private fun FavorSection(
    title: String,
    ids: List<Int>,
    data: DataRepository,
    onItemClick: (ItemInfo) -> Unit,
) {
    Spacer(Modifier.size(8.dp))
    SmallTitle(text = title)
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
