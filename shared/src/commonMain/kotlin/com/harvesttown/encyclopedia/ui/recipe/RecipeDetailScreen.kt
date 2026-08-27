package com.harvesttown.encyclopedia.ui.recipe

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
import com.harvesttown.encyclopedia.data.model.RecipeInfo
import com.harvesttown.encyclopedia.ui.components.ExpandableRichText
import com.harvesttown.encyclopedia.ui.components.MenuCard
import com.harvesttown.encyclopedia.ui.components.SpriteImage
import com.harvesttown.encyclopedia.ui.nav.LocalDataRepository
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 配方详情（以 [OverlayDialog] 承载，由列表页控制显隐）。
 * 展示产物图标、原料列表（[com.harvesttown.encyclopedia.data.model.RecipeMaterial] 用 [SpriteImage] + 名称）、
 * 解锁条件与富文本描述。
 *
 * @param recipe 当前选中的配方；为 null 时对话框不显示。
 * @param onDismissRequest 关闭回调。
 */
@Composable
fun RecipeDetailScreen(recipe: RecipeInfo?, onDismissRequest: () -> Unit) {
    OverlayDialog(
        show = recipe != null,
        onDismissRequest = onDismissRequest,
        title = recipe?.name,
    ) {
        if (recipe != null) {
            RecipeDetailBody(recipe = recipe)
        }
    }
}

@Composable
private fun RecipeDetailBody(recipe: RecipeInfo) {
    val data = LocalDataRepository.current
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

        if (recipe.materials.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            SmallTitle(text = "原料")
            Card(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp),
            ) {
                recipe.materials.forEach { material ->
                    val matItem = data.itemById(material.id)
                    MenuCard(
                        title = matItem?.name ?: "#${material.id}",
                        summary = "×${material.num}",
                        startContent = {
                            matItem?.let {
                                SpriteImage(frameKey = it.iconFrameKey, modifier = Modifier.size(48.dp))
                            }
                        },
                        onClick = {},
                    )
                }
            }
        }

        if (!recipe.deblockingDesc.isNullOrBlank()) {
            Spacer(Modifier.height(16.dp))
            SmallTitle(text = "解锁条件")
            Text(
                text = recipe.deblockingDesc,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }

        if (!recipe.descRaw.isNullOrBlank()) {
            Spacer(Modifier.height(16.dp))
            ExpandableRichText(raw = recipe.descRaw)
        }
    }
}
