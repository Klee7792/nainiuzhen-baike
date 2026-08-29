package com.nainiuzhen.wiki.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.nainiuzhen.wiki.ui.components.MenuCard
import com.nainiuzhen.wiki.ui.components.NpcPortraitImage
import com.nainiuzhen.wiki.ui.components.SpriteImage
import com.nainiuzhen.wiki.ui.nav.LocalDataRepository
import com.nainiuzhen.wiki.ui.nav.LocalNavigator
import com.nainiuzhen.wiki.ui.nav.Route
import com.nainiuzhen.wiki.utils.OnResumeEffect
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 主页内容区（底栏第 1 页「主页」的主体，无自身顶栏/底栏，由 [MainScreen] 包裹）。
 * 3 个图鉴板块入口：物品大全 / 配方查询 / NPC 资料，点击 push 对应子页。
 *
 * v6 变更（变更点 #34）：
 * - 三板块图标全部随机取一张，且「只要离开主页（子页 / 设置页 / 后台），回来都会重新随机刷新」。
 *   通过 `seed` 计数器 + 两个触发源实现：① 返回栈顶变回 [Route.Main]（从子页返回）；
 *   ② Activity ON_RESUME（从后台返回）。设置页以底栏 Tab 切换呈现，切换回主页时
 *   [HomeContent] 重新进入组合，`remember(seed)` 自然重新随机。
 * - 三张卡片图标左对齐一致：物品 / 配方图示尺寸对齐 NPC 立绘（64.dp 等效），标题左对齐。
 */
@Composable
fun HomeContent(innerPadding: PaddingValues, scrollBehavior: ScrollBehavior) {
    val navigator = LocalNavigator.current
    val data = LocalDataRepository.current
    // 随机种子：每次 reroll 重新抽取三板块图标（变更点 #34）。
    var seed by remember { mutableStateOf(0) }
    val firstItem = remember(seed, data) { data.items.randomOrNull() }
    val firstRecipe = remember(seed, data) { data.recipes.randomOrNull() }
    val randomNpc = remember(seed, data) { data.npcs.randomOrNull() }
    val reroll: () -> Unit = { seed++ }

    // 顶栏收起行为（bug-v6 line 76）：主页只有 3 个板块且通常一屏装得下，
    // 此时不允许上下滚动收起展开顶栏；仅当板块超出屏幕高度、列表可滚动时，
    // 才挂上 nestedScroll 连接让顶栏随滚动收起。
    val homeListState = rememberLazyListState()
    val homeCanScroll by remember {
        derivedStateOf { homeListState.canScrollForward || homeListState.canScrollBackward }
    }

    // 触发①：返回栈顶变回主页（从子页 / 关于返回）。NavBackStack 是 SnapshotStateList，
    // 读取 lastOrNull() 使本组合订阅其变化。
    val topRoute = navigator.backStack.lastOrNull()
    LaunchedEffect(topRoute) {
        if (topRoute is Route.Main) reroll()
    }
    // 触发②：从后台返回前台（ON_RESUME）重新随机。
    OnResumeEffect { reroll() }

    LazyColumn(
        state = homeListState,
        modifier = Modifier
            .fillMaxHeight()
            .then(
                if (homeCanScroll) Modifier.nestedScroll(scrollBehavior.nestedScrollConnection) else Modifier,
            ),
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
                            Box(
                                modifier = Modifier
                                    .size(92.dp)
                                    .background(
                                        color = MiuixTheme.colorScheme.surfaceContainerHighest,
                                        shape = RoundedCornerShape(16.dp),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                firstItem?.let {
                                    SpriteImage(
                                        frameKey = it.iconFrameKey,
                                        // [TEMP] 临时红框：观察图片本体相对淡色底托的位置关系，后续可删
                                        modifier = Modifier
                                            .size(56.dp)
                                            .border(2.dp, Color.Red, RoundedCornerShape(10.dp)),
                                    )
                                }
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
                            Box(
                                modifier = Modifier
                                    .size(92.dp)
                                    .background(
                                        color = MiuixTheme.colorScheme.surfaceContainerHighest,
                                        shape = RoundedCornerShape(16.dp),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                firstRecipe?.let {
                                    SpriteImage(
                                        frameKey = it.iconFrameKey,
                                        // [TEMP] 临时红框：观察图片本体相对淡色底托的位置关系，后续可删
                                        modifier = Modifier
                                            .size(56.dp)
                                            .border(2.dp, Color.Red, RoundedCornerShape(10.dp)),
                                    )
                                }
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
                            Box(
                                modifier = Modifier
                                    .size(92.dp)
                                    .background(
                                        color = MiuixTheme.colorScheme.surfaceContainerHighest,
                                        shape = RoundedCornerShape(16.dp),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                randomNpc?.let {
                                    // [TEMP] 临时红框：观察图片本体相对淡色底托的位置关系，后续可删
                                    NpcPortraitImage(
                                        npcId = it.id,
                                        modifier = Modifier
                                            .size(92.dp)
                                            .border(2.dp, Color.Red, RoundedCornerShape(12.dp)),
                                    )
                                }
                            }
                        },
                        onClick = { navigator.push(Route.NpcList) },
                    )
                }
            }
        }
    }
}
