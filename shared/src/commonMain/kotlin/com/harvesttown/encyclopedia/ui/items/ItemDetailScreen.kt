package com.harvesttown.encyclopedia.ui.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.harvesttown.encyclopedia.data.model.ItemInfo
import kotlin.math.roundToInt
import com.harvesttown.encyclopedia.ui.components.ExpandableRichText
import com.harvesttown.encyclopedia.ui.components.SpriteImage
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 物品详情（以 [OverlayDialog] 承载，由列表页控制显隐）。
 *
 * 布局（满足最初需求）：
 * 1. 名称·售价区：左侧卡片大小素材图；右侧名称、售价、售卖箱价，以及有售价时的白星/金星/紫星倍率卡片（四舍五入取整）。
 * 2. 描述区：富文本，限制高度、超出滚动。
 * 3. 来源行（get_way）。
 * 4. 按钮行：关闭（也可点击空白处关闭）。
 *
 * 关键：根容器用 `heightIn(max = 有限值)` 把 `OverlayDialog` 的 `Infinity` 约束收敛，
 * 嵌套的 `verticalScroll` 才能正常测量，避免闪退。
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
            ItemDetailBody(item = item, onDismissRequest = onDismissRequest)
        }
    }
}

@Composable
private fun ItemDetailBody(item: ItemInfo, onDismissRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 640.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        // 1. 名称·售价区
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
                if (item.sellboxPrice != null) {
                    Spacer(Modifier.size(2.dp))
                    Text(
                        text = "售卖箱：${item.sellboxPrice}",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
                if (item.price != null) {
                    Spacer(Modifier.size(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        StarPriceCard(label = "白星", value = (item.price * 1.2f).roundToInt())
                        StarPriceCard(label = "金星", value = (item.price * 1.5f).roundToInt())
                        StarPriceCard(label = "紫星", value = (item.price * 2f).roundToInt())
                    }
                }
            }
        }

        // 2. 描述区
        Spacer(Modifier.size(12.dp))
        ExpandableRichText(raw = item.descRaw, maxLines = 8)

        // 3. 来源行
        if (!item.source.isNullOrBlank()) {
            Spacer(Modifier.size(12.dp))
            Text(
                text = "来源：${item.source}",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }

        // 4. 按钮行
        Spacer(Modifier.size(16.dp))
        TextButton(
            text = "关闭",
            onClick = onDismissRequest,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** 单张星级倍率卡片：标签 + 计算后的售价（四舍五入取整）。 */
@Composable
private fun StarPriceCard(label: String, value: Int) {
    Card(
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .width(72.dp),
    ) {
        BasicComponent(title = label, summary = "$value")
    }
}
