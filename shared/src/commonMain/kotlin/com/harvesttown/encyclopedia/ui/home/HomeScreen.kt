package com.harvesttown.encyclopedia.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.harvesttown.encyclopedia.ui.components.MenuCard
import com.harvesttown.encyclopedia.ui.components.NpcPortraitImage
import com.harvesttown.encyclopedia.ui.components.SpriteImage
import com.harvesttown.encyclopedia.ui.nav.LocalDataRepository
import com.harvesttown.encyclopedia.ui.nav.LocalNavigator
import com.harvesttown.encyclopedia.ui.nav.Route
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.ScrollBehavior

/**
 * 主页内容区（底栏第 1 页「主页」的主体，无自身顶栏/底栏，由 [MainScreen] 包裹）。
 * 3 个图鉴板块入口：物品大全 / 配方查询 / NPC 资料，点击 push 对应子页。
 * 三板块各自为独立纯白卡片（变更点 #5），NPC 卡片左侧立绘放大（#6）并按进入随机刷新（#7）。
 */
@Composable
fun HomeContent(innerPadding: PaddingValues, scrollBehavior: ScrollBehavior) {
    val navigator = LocalNavigator.current
    val data = LocalDataRepository.current
    val firstItem = data.items.firstOrNull()
    val firstRecipe = data.recipes.firstOrNull()
    // 每次进入主页随机取一张 NPC 立绘（变更点 #7），进程存活且数据不变时保持稳定。
    val randomNpc = remember(data) { data.npcs.randomOrNull() }

    LazyColumn(
        modifier = Modifier
            .fillMaxHeight()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        contentPadding = PaddingValues(top = innerPadding.calculateTopPadding(), bottom = 12.dp),
    ) {
        item(key = "boards") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
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
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                ) {
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
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                ) {
                    MenuCard(
                        title = "NPC 资料",
                        summary = "村民喜好与日程安排",
                        startContent = {
                            randomNpc?.let {
                                NpcPortraitImage(npcId = it.id, modifier = Modifier.size(64.dp))
                            }
                        },
                        onClick = { navigator.push(Route.NpcList) },
                    )
                }
            }
        }
    }
}
