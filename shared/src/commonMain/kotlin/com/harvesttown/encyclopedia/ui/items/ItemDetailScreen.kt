package com.harvesttown.encyclopedia.ui.items

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
import com.harvesttown.encyclopedia.data.model.ItemInfo
import com.harvesttown.encyclopedia.ui.components.ExpandableRichText
import com.harvesttown.encyclopedia.ui.components.SpriteImage
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 物品详情（以 [OverlayDialog] 承载，由列表页控制显隐）。
 *
 * @param item 当前选中的物品；为 null 时对话框不显示。
 * @param onDismissRequest 关闭回调。
 */
@Composable
fun ItemDetailScreen(item: ItemInfo?, onDismissRequest: () -> Unit) {
    OverlayDialog(
        show = item != null,
        onDismissRequest = onDismissRequest,
        title = item?.name,
    ) {
        if (item != null) {
            ItemDetailBody(item = item)
        }
    }
}

@Composable
private fun ItemDetailBody(item: ItemInfo) {
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
            SpriteImage(frameKey = item.iconFrameKey, modifier = Modifier.size(64.dp))
            Column {
                Text(
                    text = item.name,
                    style = MiuixTheme.textStyles.title4,
                    color = MiuixTheme.colorScheme.onBackground,
                )
                Text(
                    text = item.categoryLabel,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
        if (item.price != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "价格：${item.price}",
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurface,
            )
        }
        if (item.sellboxPrice != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "售卖箱价格：${item.sellboxPrice}",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
        if (!item.source.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "来源：${item.source}",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
        Spacer(Modifier.height(12.dp))
        ExpandableRichText(raw = item.descRaw)
    }
}
