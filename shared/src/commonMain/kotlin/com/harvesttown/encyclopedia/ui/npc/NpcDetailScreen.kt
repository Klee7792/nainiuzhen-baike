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
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.harvesttown.encyclopedia.data.model.ItemInfo
import com.harvesttown.encyclopedia.data.model.NpcInfo
import com.harvesttown.encyclopedia.data.repository.DataRepository
import com.harvesttown.encyclopedia.ui.components.BasicDetailDialog
import com.harvesttown.encyclopedia.ui.components.ExpandableRichText
import com.harvesttown.encyclopedia.ui.components.FadeEdges
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
 * NPC 详情（以 [BasicDetailDialog] 承载，由列表页控制显隐；去标题栏、按钮置底）。
 *
 * v6 布局（变更点 #33）：
 * 1. 立绘与右侧 4 行信息等高（统一 120.dp），立绘 [ContentScale.Fit] 保持比例。
 * 2. 人物故事背景：固定区域，底部加淡入淡出（渐变到背景色），内部独立滚动。
 * 3. 最爱 / 喜欢 / 讨厌：标题加区分色；超过 4 张时横向滚动并两侧淡入淡出（[FadeEdges]）。
 * 4. 按钮区：日程 + 关闭各约 50% 宽度，居中、有间距。
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

    // 1. 立绘 + 基础信息（等高 120.dp，立绘保持比例）
    Row(
        modifier = Modifier.height(120.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NpcPortraitImage(
            npcId = npc.id,
            modifier = Modifier.height(120.dp),
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

    // 2. 人物故事背景（固定区域 + 底部淡入淡出 + 内部独立滚动）
    Spacer(Modifier.size(12.dp))
    Box(modifier = Modifier.fillMaxWidth()) {
        ExpandableRichText(raw = npc.descRaw, maxLines = 8, modifier = Modifier.fillMaxWidth())
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(20.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, MiuixTheme.colorScheme.surface),
                    ),
                ),
        )
    }

    // 3. 最爱 / 喜欢 / 讨厌（区分色，>4 张横向滚动 + 两侧淡入淡出）
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
