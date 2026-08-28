package com.harvesttown.encyclopedia.ui.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.harvesttown.encyclopedia.data.StarPricePolicy
import com.harvesttown.encyclopedia.data.model.ItemInfo
import com.harvesttown.encyclopedia.ui.components.BasicDetailDialog
import com.harvesttown.encyclopedia.ui.components.ExpandableRichText
import com.harvesttown.encyclopedia.ui.components.SpriteImage
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 物品详情（以 [BasicDetailDialog] 承载，由列表页控制显隐；去标题栏、按钮置底）。
 *
 * v6 布局（对照 v5 图7，变更点 #32）：
 * 1. 名称·素材图区（单行区块）：左侧图示（56.dp，与未点开网格卡片一致）；右侧上 16sp 名称，
 *    下平均分成 4 列（售价 / 白星素材 / 金星素材 / 紫星素材），每列上下两层 8sp 不换行。
 * 2. 描述区：富文本，固定最小高度（约 120.dp），超出内部滚动。
 * 3. 来源行：加主题强调色（primary）以突出。
 * 4. 按钮区：关闭按钮居中、100% 宽度（对话框已提供左右内边距）。
 *
 * @param item 当前选中的物品；为 null 时对话框不显示。
 * @param onDismissRequest 关闭回调。
 */
@Composable
fun ItemDetailScreen(item: ItemInfo?, onDismissRequest: () -> Unit) {
    BasicDetailDialog(
        show = item != null,
        onDismissRequest = onDismissRequest,
        buttons = {
            TextButton(
                text = "关闭",
                onClick = onDismissRequest,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        item?.let { ItemDetailBody(item = it) }
    }
}

@Composable
private fun ItemDetailBody(item: ItemInfo) {
    // 1. 名称·素材图区（左图示 + 右 名称/4列售价）
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SpriteImage(frameKey = item.iconFrameKey, modifier = Modifier.size(56.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = item.name,
                fontSize = 16.sp,
                color = MiuixTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(8.dp))
            // 4 列：售价 / 白星素材 / 金星素材 / 紫星素材
            val whitelisted = item.categoryLabel in StarPricePolicy.WHITELIST
            val p = item.price ?: 0
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                PriceColumn(label = "售价", value = if (item.price != null) "${item.price}金币" else "—")
                PriceColumn(
                    label = "白星素材",
                    value = if (whitelisted && item.price != null) "1.2倍${(p * 1.2).toInt()}金币" else "—",
                )
                PriceColumn(
                    label = "金星素材",
                    value = if (whitelisted && item.price != null) "1.5倍${(p * 1.5).toInt()}金币" else "—",
                )
                PriceColumn(
                    label = "紫星素材",
                    value = if (whitelisted && item.price != null) "2倍${p * 2}金币" else "—",
                )
            }
        }
    }

    // 2. 描述区（固定最小高度，超出滚动）
    Spacer(Modifier.height(12.dp))
    Box(modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp)) {
        ExpandableRichText(raw = item.descRaw, maxLines = 8)
    }

    // 3. 来源行（主题强调色）
    if (!item.source.isNullOrBlank()) {
        Spacer(Modifier.height(12.dp))
        Text(
            text = "来源：${item.source}",
            fontSize = 13.sp,
            color = MiuixTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** 售价信息单列：上标签 + 下数值，8sp，不换行，居中。 */
@Composable
private fun PriceColumn(label: String, value: String) {
    Column(
        modifier = Modifier.width(72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            fontSize = 8.sp,
            textAlign = TextAlign.Center,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            maxLines = 1,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 8.sp,
            textAlign = TextAlign.Center,
            color = MiuixTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}
