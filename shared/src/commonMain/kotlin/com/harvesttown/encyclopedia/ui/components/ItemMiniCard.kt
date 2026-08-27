package com.harvesttown.encyclopedia.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harvesttown.encyclopedia.data.model.ItemInfo
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 迷你物品卡片：用于配方原料区 / 产物区、NPC 最爱·喜欢·讨厌区。
 * 正方形切片 + 名称（单行省略）+ 可选数量角标。点击回调用于穿透到物品详情。
 *
 * @param item 物品（为 null 时显示占位名称）。
 * @param num 数量（如配方原料数量）；为 null 不显示。
 * @param onClick 点击回调。
 */
@Composable
fun ItemMiniCard(
    item: ItemInfo?,
    num: Int? = null,
    onClick: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .width(64.dp)
            .clickable(onClick = onClick)
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        SpriteImage(
            frameKey = item?.iconFrameKey ?: "",
            modifier = Modifier.size(48.dp),
        )
        Text(
            text = item?.name ?: "#?",
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MiuixTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 2.dp),
        )
        if (num != null) {
            Text(
                text = "×$num",
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}

/**
 * 物品卡片行（NPC 最爱 / 喜欢 / 讨厌区、配方原料区等复用）：
 * ≤4 张时整体水平居中（不裁切、不滚）；超过 4 张则横向滚动。
 * 卡片可点击穿透到物品详情。
 *
 * @param items 物品列表（元素可为 null，对应占位）。
 * @param onItemClick 点击回调（仅非空物品触发）。
 */
@Composable
fun ItemCardRow(
    items: List<ItemInfo?>,
    modifier: Modifier = Modifier,
    onItemClick: (ItemInfo) -> Unit = {},
) {
    if (items.isEmpty()) return
    if (items.size <= 4) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            items.forEach { item ->
                ItemMiniCard(item = item, onClick = { if (item != null) onItemClick(item) })
            }
        }
    } else {
        LazyRow(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items.size) { index ->
                val item = items[index]
                ItemMiniCard(item = item, onClick = { if (item != null) onItemClick(item) })
            }
        }
    }
}
