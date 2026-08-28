package com.nainiuzhen.wiki.ui.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nainiuzhen.wiki.data.StarPricePolicy
import com.nainiuzhen.wiki.data.StarTier
import com.nainiuzhen.wiki.data.model.ItemInfo
import com.nainiuzhen.wiki.ui.components.BasicDetailDialog
import com.nainiuzhen.wiki.ui.components.RichText
import com.nainiuzhen.wiki.ui.components.SpriteImage
import com.nainiuzhen.wiki.ui.components.StarImage
import com.nainiuzhen.wiki.ui.recipe.SectionDivider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 名称相对左侧图示再右移的距离（约一个 16sp 汉字宽）。 */
private val ITEM_NAME_INDENT = 16.dp

/** 星级图示尺寸。 */
private val STAR_IMAGE_SIZE = 16.dp

/**
 * 物品详情（以 [BasicDetailDialog] 承载，由列表页控制显隐；去标题栏、按钮置底）。
 *
 * v6-bug 布局（物品大全页 · 详情 dialog）：
 * 1. 名称·素材图区（顶栏位，其下方不加分割线）：左侧图示（56.dp，与未点开网格卡片一致）；
 *    右侧上 16sp 名称（再右移 [ITEM_NAME_INDENT]，与图示留出一个汉字的距离），
 *    下为售价行（[ItemPriceRow]）：「售价」列 + 白 / 金 / 紫三档星级图示列，
 *    每列图示或标签之上、价格之下，价格只显示最终计算结果（如 `27金币`，不再显示 `1.5倍27金币`）。
 * 2. 描述区：富文本，**按内容行数自适应高度**（不再固定高度、不再内部滚动）。
 * 3. 来源行：加主题强调色（primary）以突出。
 * 4. 按钮区：关闭按钮居中、100% 宽度（对话框已提供左右内边距），其与上方区域之间不加分割线。
 *
 * 区域之间使用配方页的 [SectionDivider]（宽度 80%、水平居中），保证三个板块完全一致。
 * 分割线只在**实际渲染出的**相邻区块之间插入（名称区下方恒不加，按钮区上方恒不加），
 * 避免出现区块为空却残留一条分割线的情况。整个 dialog 的高度由内容决定。
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
    // 只有非空区块才渲染，避免「区块为空但分割线还在」的孤零零一条线。
    val hasDesc = item.descRaw.isNotBlank()
    val hasSource = !item.source.isNullOrBlank()

    // 1. 名称·素材图区（顶栏位：其下方不加分割线）
    ItemHeaderSection(item = item)

    // 2. 描述区（自适应高度）
    if (hasDesc) {
        SectionDivider()
        RichText(
            raw = item.descRaw,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    // 3. 来源行（主题强调色）
    if (hasSource) {
        SectionDivider()
        Text(
            text = "来源：${item.source}",
            fontSize = 13.sp,
            color = MiuixTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * 名称·素材图区：左侧图示 + 右侧名称与售价行。
 *
 * 名称额外右移 [ITEM_NAME_INDENT]，与左侧图示保持约一个汉字的间距。
 */
@Composable
private fun ItemHeaderSection(item: ItemInfo) {
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
                modifier = Modifier.padding(start = ITEM_NAME_INDENT),
            )
            Spacer(Modifier.height(8.dp))
            ItemPriceRow(price = item.price, categoryLabel = item.categoryLabel)
        }
    }
}

/**
 * 售价行：
 * - 物品无售价，或类别不在 [StarPricePolicy.WHITELIST] 内时，只显示基础售价列（左对齐）。
 * - 命中白名单时显示 4 等分列：「售价」+ 白 / 金 / 紫三档星级图示，
 *   每列下方是该档**最终计算结果**（[StarTier.value]），不再展示倍率前缀。
 */
@Composable
private fun ItemPriceRow(price: Int?, categoryLabel: String) {
    val priceText = if (price != null) "${price}金币" else "—"
    val tiers = StarPricePolicy.tiers(price, categoryLabel)
    if (tiers == null) {
        // 与上方名称左对齐（名称已右移 [ITEM_NAME_INDENT]）。
        Column(modifier = Modifier.fillMaxWidth().padding(start = ITEM_NAME_INDENT)) {
            Text(
                text = "售价",
                fontSize = 9.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = priceText,
                fontSize = 11.sp,
                color = MiuixTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val cell = Modifier.weight(1f)
            PriceCell(
                modifier = cell,
                label = "售价",
                value = priceText,
            )
            tiers.forEach { tier ->
                StarPriceCell(
                    modifier = cell,
                    tier = tier,
                    value = if (price != null) "${tier.value(price)}金币" else "—",
                )
            }
        }
    }
}

/** 普通售价列：上标签（9sp）+ 下数值（11sp），居中、不换行。 */
@Composable
private fun PriceCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            maxLines = 1,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            color = MiuixTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

/** 星级售价列：上星级图示（[StarImage]）+ 下最终价格（11sp），居中、不换行。 */
@Composable
private fun StarPriceCell(
    tier: StarTier,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StarImage(
            level = tier.imageLevel,
            modifier = Modifier.size(STAR_IMAGE_SIZE),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            color = MiuixTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}
