package com.harvesttown.encyclopedia.ui.recipe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
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
import com.harvesttown.encyclopedia.data.model.RecipeInfo
import com.harvesttown.encyclopedia.ui.components.ExpandableRichText
import com.harvesttown.encyclopedia.ui.components.ItemMiniCard
import com.harvesttown.encyclopedia.ui.components.SpriteImage
import com.harvesttown.encyclopedia.ui.items.ItemDetailScreen
import com.harvesttown.encyclopedia.ui.nav.LocalDataRepository
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 配方详情（以 [OverlayDialog] 承载，由列表页控制显隐）。
 *
 * 布局（满足最初需求）：
 * 1. 名称区（图纸 / 菜谱无售价）：左侧产物图标，右侧名称 + 类型。
 * 2. 描述区：富文本，限制高度、超出滚动。
 * 3. 来源 / 解锁条件（get_way）。
 * 4. 原料区：一行 n 列物品卡片（≤4 列时居中，超出则横向滚动），点击穿透到物品详情。
 * 5. 产物区：该配方做出的产物卡片（同样可点击查看详情）。
 * 6. 按钮行：关闭（也可点击空白处关闭）。
 *
 * 关键：根容器用 `heightIn(max = 有限值)` 把 [OverlayDialog] 的 `Infinity` 约束收敛，
 * 嵌套的 `verticalScroll` 才能正常测量，避免闪退。原料 / 产物卡片点击会再弹出一个物品详情对话框。
 *
 * @param recipe 当前选中的配方；为 null 时对话框不显示。
 * @param onDismissRequest 关闭回调。
 */
@Composable
fun RecipeDetailScreen(recipe: RecipeInfo?, onDismissRequest: () -> Unit) {
    var nestedItem by remember { mutableStateOf<ItemInfo?>(null) }
    OverlayDialog(
        show = recipe != null,
        onDismissRequest = onDismissRequest,
        title = recipe?.name,
    ) {
        if (recipe != null) {
            RecipeDetailBody(
                recipe = recipe,
                onItemClick = { nestedItem = it },
                onDismissRequest = onDismissRequest,
            )
        }
    }
    // 原料 / 产物卡片穿透出的物品详情（叠加在配方对话框之上）
    ItemDetailScreen(item = nestedItem, onDismissRequest = { nestedItem = null })
}

@Composable
private fun RecipeDetailBody(
    recipe: RecipeInfo,
    onItemClick: (ItemInfo) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val data = LocalDataRepository.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 640.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
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

        // 2. 描述区
        if (!recipe.descRaw.isNullOrBlank()) {
            SpacerH(12.dp)
            ExpandableRichText(raw = recipe.descRaw, maxLines = 8)
        }

        // 3. 来源 / 解锁条件
        if (!recipe.deblockingDesc.isNullOrBlank()) {
            SpacerH(16.dp)
            SmallTitle(text = "来源 / 解锁")
            Text(
                text = recipe.deblockingDesc,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }

        // 4. 原料区
        if (recipe.materials.isNotEmpty()) {
            SpacerH(16.dp)
            SmallTitle(text = "原料")
            val mats = recipe.materials.map { data.itemById(it.id) to it.num }
            MaterialCardRow(
                items = mats,
                modifier = Modifier.padding(horizontal = 12.dp),
                onItemClick = onItemClick,
            )
        }

        // 5. 产物区
        SpacerH(16.dp)
        SmallTitle(text = "产物")
        val product = data.itemById(recipe.target)
        MaterialCardRow(
            items = listOf(product to recipe.targetNum),
            modifier = Modifier.padding(horizontal = 12.dp),
            onItemClick = onItemClick,
        )

        // 6. 按钮行
        SpacerH(16.dp)
        TextButton(
            text = "关闭",
            onClick = onDismissRequest,
            modifier = Modifier.fillMaxWidth(),
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
    modifier: Modifier = Modifier,
    onItemClick: (ItemInfo) -> Unit,
) {
    if (items.size <= 4) {
        Row(
            modifier = modifier.fillMaxWidth(),
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
            modifier = modifier.fillMaxWidth(),
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

/** 竖直间距（避免与 miuix 自带 Spacer 语义混淆，单独封装）。 */
@Composable
private fun SpacerH(height: androidx.compose.ui.unit.Dp) {
    Spacer(Modifier.height(height))
}
