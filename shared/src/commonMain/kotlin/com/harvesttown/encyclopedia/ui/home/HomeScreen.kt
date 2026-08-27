package com.harvesttown.encyclopedia.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 主页：3 个图鉴板块入口（物品大全 / 配方查询 / NPC 资料）+ 设置入口。
 * 每个入口用 [MenuCard]，左侧正方形素材取自对应板块的随机图标 / 立绘。
 */
@Composable
fun HomeScreen() {
    val navigator = LocalNavigator.current
    val data = LocalDataRepository.current
    val scrollBehavior = MiuixScrollBehavior()
    Scaffold(
        topBar = {
            SmallTopAppBar(title = "奶牛镇百科", scrollBehavior = scrollBehavior)
        },
    ) { innerPadding ->
        val firstItem = data.items.firstOrNull()
        val firstRecipe = data.recipes.firstOrNull()
        val firstNpc = data.npcs.firstOrNull()
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
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
                    MenuCard(
                        title = "设置",
                        summary = "清理缓存与关于",
                        startContent = {
                            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = MiuixIcons.Settings,
                                    contentDescription = null,
                                    tint = MiuixTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                        },
                        onClick = { navigator.push(Route.Settings) },
                    )
                }
            }
        }
    }
}
