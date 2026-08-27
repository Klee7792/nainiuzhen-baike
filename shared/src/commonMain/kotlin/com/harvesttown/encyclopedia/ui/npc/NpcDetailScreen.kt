package com.harvesttown.encyclopedia.ui.npc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.harvesttown.encyclopedia.data.model.NpcInfo
import com.harvesttown.encyclopedia.ui.components.ExpandableRichText
import com.harvesttown.encyclopedia.ui.components.MenuCard
import com.harvesttown.encyclopedia.ui.components.NpcPortraitImage
import com.harvesttown.encyclopedia.ui.components.SpriteImage
import com.harvesttown.encyclopedia.ui.nav.LocalDataRepository
import com.harvesttown.encyclopedia.ui.nav.LocalNavigator
import com.harvesttown.encyclopedia.ui.nav.Route
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * NPC 详情（以 [OverlayDialog] 承载，由列表页控制显隐）。
 *
 * 展示立绘、简介、**[好感度]**（[NpcInfo.loveItemId]，即该 NPC 自己的好感度所对应的 items id，
 * 区别于「喜欢 / 最爱 / 讨厌」列表）、最爱 / 喜欢 / 讨厌分区，以及（当 [NpcInfo.maxStar] > 0 时）
 * 跳转日程的入口。
 *
 * @param npc 当前选中的 NPC；为 null 时对话框不显示。
 * @param onDismissRequest 关闭回调。
 */
@Composable
fun NpcDetailScreen(npc: NpcInfo?, onDismissRequest: () -> Unit) {
    OverlayDialog(
        show = npc != null,
        onDismissRequest = onDismissRequest,
        title = npc?.name,
    ) {
        if (npc != null) {
            NpcDetailBody(npc = npc, onDismissRequest = onDismissRequest)
        }
    }
}

@Composable
private fun NpcDetailBody(npc: NpcInfo, onDismissRequest: () -> Unit) {
    val navigator = LocalNavigator.current
    val data = LocalDataRepository.current
    val loveItem = data.itemById(npc.loveItemId)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NpcPortraitImage(npcId = npc.id, modifier = Modifier.size(96.dp))
            Column {
                Text(
                    text = npc.name,
                    style = MiuixTheme.textStyles.title4,
                    color = MiuixTheme.colorScheme.onBackground,
                )
                Text(
                    text = "${npc.address} · ${npc.birthday}",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }

        if (npc.descRaw.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            ExpandableRichText(raw = npc.descRaw)
        }

        // 好感度（loveItemId 本身是一个 items 的 id，送礼增长的是它；不是「喜欢的物品」）
        Spacer(Modifier.height(16.dp))
        SmallTitle(text = "好感度")
        Card(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp),
        ) {
            MenuCard(
                title = "好感度",
                summary = loveItem?.name ?: "#${npc.loveItemId}",
                startContent = {
                    SpriteImage(
                        frameKey = loveItem?.iconFrameKey ?: npc.loveItemId.toString(),
                        modifier = Modifier.size(48.dp),
                    )
                },
                onClick = {},
            )
        }

        // 最爱
        if (npc.bestFavorItems.isNotEmpty()) {
            SmallTitle(text = "最爱")
            Card(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp),
            ) {
                npc.bestFavorItems.forEach { id ->
                    val item = data.itemById(id)
                    MenuCard(
                        title = item?.name ?: "#$id",
                        summary = "",
                        startContent = {
                            item?.let {
                                SpriteImage(frameKey = it.iconFrameKey, modifier = Modifier.size(48.dp))
                            }
                        },
                        onClick = {},
                    )
                }
            }
        }

        // 喜欢
        if (npc.likeItems.isNotEmpty()) {
            SmallTitle(text = "喜欢")
            Card(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp),
            ) {
                npc.likeItems.forEach { id ->
                    val item = data.itemById(id)
                    MenuCard(
                        title = item?.name ?: "#$id",
                        summary = "",
                        startContent = {
                            item?.let {
                                SpriteImage(frameKey = it.iconFrameKey, modifier = Modifier.size(48.dp))
                            }
                        },
                        onClick = {},
                    )
                }
            }
        }

        // 讨厌
        if (npc.hateItems.isNotEmpty()) {
            SmallTitle(text = "讨厌")
            Card(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp),
            ) {
                npc.hateItems.forEach { id ->
                    val item = data.itemById(id)
                    MenuCard(
                        title = item?.name ?: "#$id",
                        summary = "",
                        startContent = {
                            item?.let {
                                SpriteImage(frameKey = it.iconFrameKey, modifier = Modifier.size(48.dp))
                            }
                        },
                        onClick = {},
                    )
                }
            }
        }

        // 日程（maxStar > 0 可进入，否则禁用）
        Spacer(Modifier.height(4.dp))
        SmallTitle(text = "日程")
        Card(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp),
        ) {
            BasicComponent(
                title = "查看日程",
                summary = if (npc.maxStar > 0) "好感度 ${npc.maxStar} 心" else "暂未开放",
                enabled = npc.maxStar > 0,
                onClick = {
                    if (npc.maxStar > 0) {
                        navigator.push(Route.NpcSchedule(npc.id))
                        onDismissRequest()
                    }
                },
            )
        }
    }
}
