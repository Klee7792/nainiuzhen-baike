package com.harvesttown.encyclopedia.ui.npc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.harvesttown.encyclopedia.data.model.ItemInfo
import com.harvesttown.encyclopedia.data.model.NpcInfo
import com.harvesttown.encyclopedia.data.repository.DataRepository
import com.harvesttown.encyclopedia.ui.components.ExpandableRichText
import com.harvesttown.encyclopedia.ui.components.ItemCardRow
import com.harvesttown.encyclopedia.ui.components.NpcPortraitImage
import com.harvesttown.encyclopedia.ui.components.SpriteImage
import com.harvesttown.encyclopedia.ui.items.ItemDetailScreen
import com.harvesttown.encyclopedia.ui.nav.LocalDataRepository
import com.harvesttown.encyclopedia.ui.nav.LocalNavigator
import com.harvesttown.encyclopedia.ui.nav.Route
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * NPC 详情（以 [OverlayDialog] 承载，由列表页控制显隐）。
 *
 * 布局（满足最初需求，6 + 1 区块）：
 * 1. 立绘（略缩至 ~80%）+ 右侧 住址 / 生日 / 好感 max：x 心。
 * 2. 人物故事背景（内部滚动）。
 * 3. 好感度（[NpcInfo.loveItemId]，送礼增长的那条好感度对应的物品，并非「喜欢」列表）。
 * 4. 最爱 / 5. 喜欢 / 6. 讨厌（为空也保留高度，保持对话框大小一致；卡片可点击穿透到物品详情）。
 * 7. 按钮区：左侧「日程」（[NpcInfo.maxStar] > 0 可进入，否则禁用）、右侧「关闭」。
 *
 * 关键：根容器用 `heightIn(max = 有限值)` 收敛 [OverlayDialog] 的 `Infinity` 约束，
 * 嵌套 `verticalScroll` 才能正常测量，避免闪退。
 *
 * @param npc 当前选中的 NPC；为 null 时对话框不显示。
 * @param onDismissRequest 关闭回调。
 */
@Composable
fun NpcDetailScreen(npc: NpcInfo?, onDismissRequest: () -> Unit) {
    var nestedItem by remember { mutableStateOf<ItemInfo?>(null) }
    OverlayDialog(
        show = npc != null,
        onDismissRequest = onDismissRequest,
        title = npc?.name,
    ) {
        if (npc != null) {
            NpcDetailBody(npc = npc, onItemClick = { nestedItem = it }, onDismissRequest = onDismissRequest)
        }
    }
    // 最爱 / 喜欢 / 讨厌卡片穿透出的物品详情（叠加在 NPC 对话框之上）
    ItemDetailScreen(item = nestedItem, onDismissRequest = { nestedItem = null })
}

@Composable
private fun NpcDetailBody(
    npc: NpcInfo,
    onItemClick: (ItemInfo) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val navigator = LocalNavigator.current
    val data = LocalDataRepository.current
    val loveItem = data.itemById(npc.loveItemId)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 640.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        // 1. 立绘 + 基础信息
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NpcPortraitImage(npcId = npc.id, modifier = Modifier.size(80.dp))
            Column {
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
        if (npc.descRaw.isNotBlank()) {
            Spacer(Modifier.size(12.dp))
            ExpandableRichText(raw = npc.descRaw, maxLines = 8)
        }

        // 3. 好感度（loveItemId 本身是 items 的 id，送礼增长的是它）
        Spacer(Modifier.size(16.dp))
        SmallTitle(text = "好感度")
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = { if (loveItem != null) onItemClick(loveItem) })
                    .padding(12.dp),
            ) {
                SpriteImage(
                    frameKey = loveItem?.iconFrameKey ?: npc.loveItemId.toString(),
                    modifier = Modifier.size(48.dp),
                )
                Column {
                    Text(
                        text = loveItem?.name ?: "#${npc.loveItemId}",
                        style = MiuixTheme.textStyles.body1,
                        color = MiuixTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "送礼增长的好感度",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
        }

        // 4. 最爱 / 5. 喜欢 / 6. 讨厌（空也保留高度）
        FavorSection(title = "最爱", ids = npc.bestFavorItems, data = data, onItemClick = onItemClick)
        FavorSection(title = "喜欢", ids = npc.likeItems, data = data, onItemClick = onItemClick)
        FavorSection(title = "讨厌", ids = npc.hateItems, data = data, onItemClick = onItemClick)

        // 7. 按钮区：日程（左）+ 关闭（右）
        Spacer(Modifier.size(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(
                text = "日程",
                enabled = npc.maxStar > 0,
                onClick = {
                    if (npc.maxStar > 0) {
                        navigator.push(Route.NpcSchedule(npc.id))
                        onDismissRequest()
                    }
                },
                modifier = Modifier.fillMaxWidth(0.49f),
            )
            TextButton(
                text = "关闭",
                onClick = onDismissRequest,
                modifier = Modifier.fillMaxWidth(0.49f),
            )
        }
    }
}

/** 物品列表分区（最爱 / 喜欢 / 讨厌）：标题恒显；内容为空时保留最小高度，使对话框大小一致。 */
@Composable
private fun FavorSection(
    title: String,
    ids: List<Int>,
    data: DataRepository,
    onItemClick: (ItemInfo) -> Unit,
) {
    Spacer(Modifier.size(16.dp))
    SmallTitle(text = title)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp),
    ) {
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
