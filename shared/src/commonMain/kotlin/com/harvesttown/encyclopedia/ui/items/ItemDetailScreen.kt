package com.harvesttown.encyclopedia.ui.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.harvesttown.encyclopedia.ui.components.StarImage
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 物品详情（以 [BasicDetailDialog] 承载，由列表页控制显隐；去标题栏、按钮置底，变更点 #21 / #22）。
 *
 * 布局（满足 v5 需求）：
 * 1. 名称·素材图区：左侧卡片大小素材图；右侧名称、售价。
 * 2. 价格卡区：售价卡 + 至多 3 张星级图卡（白/金/紫），上图下「xx 金币」小字（变更点 #12）。
 *    仅白名单类别（[StarPricePolicy.WHITELIST]）展示星价，否则仅售价卡（变更点 #13）。
 * 3. 描述区：富文本，限制高度、超出滚动。
 * 4. 来源行（get_way）。
 * 5. 按钮行：关闭（固定于底部，无需滚动即可点击）。
 *
 * 注意：已移除「售卖箱价格」字段（变更点 #11）。
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
                modifier = Modifier.fillMaxWidth(0.49f),
            )
        },
    ) {
        item?.let { ItemDetailBody(item = it) }
    }
}

@Composable
private fun ItemDetailBody(item: ItemInfo) {
    // 1. 名称·素材图区
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
            if (item.price != null) {
                Spacer(Modifier.size(2.dp))
                Text(
                    text = "售价：${item.price}",
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurface,
                )
            }
        }
    }

    // 2. 价格卡区（4 列并列；无星价时仅售价卡）
    Spacer(Modifier.size(12.dp))
    val tiers = StarPricePolicy.tiers(item.price, item.categoryLabel)
    if (tiers == null) {
        if (item.price != null) {
            Row(modifier = Modifier.fillMaxWidth()) {
                PriceCard(
                    topContent = {
                        Text(
                            text = "${item.price}",
                            style = MiuixTheme.textStyles.title4,
                            color = MiuixTheme.colorScheme.primary,
                        )
                    },
                    value = item.price,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            PriceCard(
                topContent = {
                    Text(
                        text = "${item.price}",
                        style = MiuixTheme.textStyles.title4,
                        color = MiuixTheme.colorScheme.primary,
                    )
                },
                value = item.price ?: 0,
                modifier = Modifier.fillMaxWidth(0.23f),
            )
            tiers.forEach { tier ->
                PriceCard(
                    topContent = { StarImage(level = tier.imageLevel, modifier = Modifier.size(24.dp)) },
                    value = tier.value(item.price ?: 0),
                    modifier = Modifier.fillMaxWidth(0.23f),
                )
            }
        }
    }

    // 3. 描述区
    Spacer(Modifier.size(12.dp))
    ExpandableRichText(raw = item.descRaw, maxLines = 8)

    // 4. 来源行
    if (!item.source.isNullOrBlank()) {
        Spacer(Modifier.size(12.dp))
        Text(
            text = "来源：${item.source}",
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}

/** 单张价格卡：上区为价格数字或星级图、下区为「xx 金币」小字（变更点 #12）。 */
@Composable
private fun PriceCard(
    topContent: @Composable () -> Unit,
    value: Int,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 4.dp),
        ) {
            topContent()
            Spacer(Modifier.size(4.dp))
            Text(
                text = "$value 金币",
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}
