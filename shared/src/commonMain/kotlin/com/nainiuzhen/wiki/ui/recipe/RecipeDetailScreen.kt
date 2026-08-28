package com.nainiuzhen.wiki.ui.recipe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nainiuzhen.wiki.data.model.ItemInfo
import com.nainiuzhen.wiki.data.model.RecipeInfo
import com.nainiuzhen.wiki.ui.components.BasicDetailDialog
import com.nainiuzhen.wiki.ui.components.ItemMiniCard
import com.nainiuzhen.wiki.ui.components.RichText
import com.nainiuzhen.wiki.ui.components.SpriteImage
import com.nainiuzhen.wiki.ui.items.ItemDetailScreen
import com.nainiuzhen.wiki.ui.nav.LocalDataRepository
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 区域分割线占内容宽度的比例（v6：分割线宽度 80%）。 */
private const val SECTION_DIVIDER_WIDTH_FRACTION = 0.8f

/**
 * 配方详情（以 [BasicDetailDialog] 承载，由列表页控制显隐；去标题栏、按钮置底，变更点 #21 / #22）。
 *
 * v7 布局（沿用 v5 分区，仅套用 v6 通用 dialog 规则）：
 * 1. 名称区（图纸 / 菜谱无售价）：左侧产物图标，右侧名称 + 类型。该区相当于「顶栏区」，
 *    按规则其下方不加分割线。
 * 2. 描述区：富文本，**不再固定高度 / 限制行数**，按内容行数自适应。
 * 3. 解锁方式：来源 / 解锁条件（deblockingDesc）。
 * 4. 原料区：一行 n 列物品卡片（≤4 列时居中，超出则横向滚动），点击穿透到物品详情。
 * 5. 产物区：该配方做出的产物卡片（同样可点击查看详情）。
 * 6. 按钮区：关闭（固定于底部，无需滚动即可点击）。按钮区上方不加分割线。
 *
 * 相邻区域之间插入 80% 宽居中的 [SectionDivider]（2↔3、3↔4、4↔5）。
 * 整个 dialog 不再固定高度：内容区由 [BasicDetailDialog] 以「有限最大值 + 滚动」收敛
 * （pit #1：[OverlayDialog][top.yukonga.miuix.kmp.overlay.OverlayDialog] 以 `Infinity`
 * 最大高度测量内容），高度按内容自适应；dialog 与底部的距离、宽度、居中状态与按钮区高度
 * 由 [BasicDetailDialog] 统一保证，与物品 / NPC 详情 dialog 一致。
 *
 * @param recipe 当前选中的配方；为 null 时对话框不显示。
 * @param onDismissRequest 关闭回调。
 */
@Composable
fun RecipeDetailScreen(recipe: RecipeInfo?, onDismissRequest: () -> Unit) {
    var nestedItem by remember { mutableStateOf<ItemInfo?>(null) }
    BasicDetailDialog(
        show = recipe != null,
        onDismissRequest = onDismissRequest,
        buttons = {
        TextButton(
            text = "关闭",
            onClick = onDismissRequest,
            modifier = Modifier.fillMaxWidth(),
        )
        },
    ) {
        recipe?.let { RecipeDetailBody(recipe = it, onItemClick = { nestedItem = it }) }
    }
    // 原料 / 产物卡片穿透出的物品详情（叠加在配方对话框之上）
    ItemDetailScreen(item = nestedItem, onDismissRequest = { nestedItem = null })
}

@Composable
private fun RecipeDetailBody(
    recipe: RecipeInfo,
    onItemClick: (ItemInfo) -> Unit,
) {
    val data = LocalDataRepository.current
    // 名称区（顶栏区）与其下方区域之间不加分割线；其后每出现一个新区域才补一条。
    // 仅作为本次组合内的顺序标记（不用 remember：每次重组都必须从 false 重新开始）。
    var hasSectionAbove = false

    // 1. 名称区（无售价）
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SpriteImage(frameKey = recipe.iconFrameKey, modifier = Modifier.size(64.dp))
        Column {
            Text(
                text = recipe.name,
                style = MiuixTheme.textStyles.title4,
                color = MiuixTheme.colorScheme.onBackground,
            )
            Text(
                text = recipe.typeLabel,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }

    // 2. 描述区：高度按内容行数自适应（不再限制 8 行、不再内部滚动）
    if (!recipe.descRaw.isNullOrBlank()) {
        Spacer(Modifier.size(8.dp))
        RichText(
            raw = recipe.descRaw,
            modifier = Modifier.fillMaxWidth(),
        )
        hasSectionAbove = true
    }

    // 3. 解锁方式（与具体解锁条件同一行，可换行；主题强调色）
    if (!recipe.deblockingDesc.isNullOrBlank()) {
        if (hasSectionAbove) {
            SectionDivider()
        } else {
            Spacer(Modifier.size(10.dp))
        }
        SmallTitle(text = "解锁方式")
        Text(
            text = recipe.deblockingDesc,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth(),
        )
        hasSectionAbove = true
    }

    // 4. 原料区
    if (recipe.materials.isNotEmpty()) {
        if (hasSectionAbove) {
            SectionDivider()
        } else {
            Spacer(Modifier.size(10.dp))
        }
        SmallTitle(text = "原料")
        val mats = recipe.materials.map { data.itemById(it.id) to it.num }
        MaterialCardRow(
            items = mats,
            onItemClick = onItemClick,
        )
        hasSectionAbove = true
    }

    // 5. 产物区（按钮区上方不加分割线）
    if (hasSectionAbove) {
        SectionDivider()
    } else {
        Spacer(Modifier.size(10.dp))
    }
    SmallTitle(text = "产物")
    val product = data.itemById(recipe.target)
    MaterialCardRow(
        items = listOf(product to recipe.targetNum),
        onItemClick = onItemClick,
    )
}

/**
 * 详情 dialog 内两个区域之间的分割线：宽度 80%、水平居中。
 *
 * 按 v6 规则，按钮区与其上方区域、顶栏区与其下方区域都不加分割线，因此调用方只在
 * 「内容区之间」调用本组件。
 *
 * @param modifier 外层修饰。
 */
@Composable
fun SectionDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(SECTION_DIVIDER_WIDTH_FRACTION),
            color = MiuixTheme.colorScheme.dividerLine,
        )
    }
}

/**
 * 物品卡片行：≤4 张时整体水平居中；超过 4 张则横向滚动。
 * 卡片大小、样式与物品区一致，点击穿透到物品详情。
 */
@Composable
private fun MaterialCardRow(
    items: List<Pair<ItemInfo?, Int>>,
    onItemClick: (ItemInfo) -> Unit,
) {
    if (items.isEmpty()) return
    if (items.size <= 4) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            items.forEach { (item, num) ->
                ItemMiniCard(
                    item = item,
                    num = num,
                    onClick = { if (item != null) onItemClick(item) },
                )
            }
        }
    } else {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items.size) { index ->
                val (item, num) = items[index]
                ItemMiniCard(
                    item = item,
                    num = num,
                    onClick = { if (item != null) onItemClick(item) },
                )
            }
        }
    }
}
