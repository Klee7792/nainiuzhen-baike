package com.harvesttown.encyclopedia.ui.recipe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
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
import com.harvesttown.encyclopedia.ui.components.BasicDetailDialog
import com.harvesttown.encyclopedia.ui.components.ExpandableRichText
import com.harvesttown.encyclopedia.ui.components.ItemMiniCard
import com.harvesttown.encyclopedia.ui.components.SpriteImage
import com.harvesttown.encyclopedia.ui.items.ItemDetailScreen
import com.harvesttown.encyclopedia.ui.nav.LocalDataRepository
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 配方详情（以 [BasicDetailDialog] 承载，由列表页控制显隐；去标题栏、按钮置底，变更点 #21 / #22）。
 *
 * v5 布局（变更点 #20）：
 * 1. 名称区（图纸 / 菜谱无售价）：左侧产物图标，右侧名称 + 类型。
 * 2. 描述区：富文本，限制高度、超出滚动。
 * 3. 来源 / 解锁条件（get_way）。
 * 4. 原料区：一行 n 列物品卡片（≤4 列时居中，超出则横向滚动），点击穿透到物品详情。
 * 5. 产物区：该配方做出的产物卡片（同样可点击查看详情）。
 * 6. 按钮区：关闭（固定于底部，无需滚动即可点击；行间距较 v4 收紧）。
 *
 * 关键：根容器用 `heightIn(max = 640.dp)` 收敛 [BasicDetailDialog] 的 `Infinity` 约束（pit #1），
 * 嵌套 `verticalScroll` 才能正常测量。原料 / 产物卡片点击会再弹出一个物品详情对话框。
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
        Spacer(Modifier.size(8.dp))
        ExpandableRichText(raw = recipe.descRaw, maxLines = 8)
    }

    // 3. 解锁方式（与具体解锁条件同一行，可换行；主题强调色）
    if (!recipe.deblockingDesc.isNullOrBlank()) {
        Spacer(Modifier.size(10.dp))
        SmallTitle(text = "解锁方式")
        Text(
            text = recipe.deblockingDesc,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    // 4. 原料区
    if (recipe.materials.isNotEmpty()) {
        Spacer(Modifier.size(10.dp))
        SmallTitle(text = "原料")
        val mats = recipe.materials.map { data.itemById(it.id) to it.num }
        MaterialCardRow(
            items = mats,
            onItemClick = onItemClick,
        )
    }

    // 5. 产物区
    Spacer(Modifier.size(10.dp))
    SmallTitle(text = "产物")
    val product = data.itemById(recipe.target)
    MaterialCardRow(
        items = listOf(product to recipe.targetNum),
        onItemClick = onItemClick,
    )
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
        androidx.compose.foundation.lazy.LazyRow(
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
