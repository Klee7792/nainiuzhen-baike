package com.harvesttown.encyclopedia.ui.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.harvesttown.encyclopedia.ui.components.MenuCard
import com.harvesttown.encyclopedia.ui.components.NpcPortraitImage
import com.harvesttown.encyclopedia.ui.components.SpriteImage
import com.harvesttown.encyclopedia.ui.nav.LocalDataRepository
import com.harvesttown.encyclopedia.ui.nav.LocalNavigator
import com.harvesttown.encyclopedia.ui.nav.Route
import top.yukonga.miuix.kmp.basic.Card

/**
 * 主页内容区（底栏第 1 页「主页」的主体，无自身顶栏/底栏，由 [MainScreen] 包裹）。
 * 3 个图鉴板块入口：物品大全 / 配方查询 / NPC 资料，点击 push 对应子页。
 */
@Composable
fun HomeContent(innerPadding: PaddingValues) {
    val navigator = LocalNavigator.current
    val data = LocalDataRepository.current
    val firstItem = data.items.firstOrNull()
    val firstRecipe = data.recipes.firstOrNull()
    val firstNpc = data.npcs.firstOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxHeight(),
        contentPadding = PaddingValues(top = innerPadding.calculateTopPadding(), bottom = 12.dp),
    ) {
        item(key = "boards") {
            Card(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp),
            ) {
                MenuCard(
                    title = "物品大全",
                    summary = "查询全部物品资料",
                    startContent = {
                        firstItem?.let {
                            SpriteImage(frameKey = it.iconFrameKey, modifier = Modifier.size(48.dp))
                        }
                    },
                    onClick = { navigator.push(Route.ItemList) },
                )
                MenuCard(
                    title = "配方查询",
                    summary = "图纸与菜谱制作指引",
                    startContent = {
                        firstRecipe?.let {
                            SpriteImage(frameKey = it.iconFrameKey, modifier = Modifier.size(48.dp))
                        }
                    },
                    onClick = { navigator.push(Route.RecipeList) },
                )
                MenuCard(
                    title = "NPC 资料",
                    summary = "村民喜好与日程安排",
                    startContent = {
                        firstNpc?.let {
                            NpcPortraitImage(npcId = it.id, modifier = Modifier.size(48.dp))
                        }
                    },
                    onClick = { navigator.push(Route.NpcList) },
                )
            }
        }
    }
}
