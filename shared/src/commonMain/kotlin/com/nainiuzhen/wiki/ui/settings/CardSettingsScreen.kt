package com.nainiuzhen.wiki.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.nainiuzhen.wiki.ui.components.AppSubPageScaffold
import com.nainiuzhen.wiki.ui.nav.LocalNavigator
import com.nainiuzhen.wiki.utils.CARD_TOGGLE_GROUPS
import com.nainiuzhen.wiki.utils.CardCorner
import com.nainiuzhen.wiki.utils.CardSection
import com.nainiuzhen.wiki.utils.LocalAppSettings
import com.nainiuzhen.wiki.utils.LocalUpdateAppSettings
import top.yukonga.miuix.kmp.anim.DecelerateEasing
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

/**
 * 卡片设置子页（从设置页「卡片设置」进入）。
 *
 * 三组装饰开关（卡片背景 / 卡片圆角 / 胶囊背景）结构完全一致，统一由
 * [com.nainiuzhen.wiki.utils.CARD_TOGGLE_GROUPS] 描述、本页循环渲染，避免三份重复 UI。
 *
 * ## 布局（每组恰好一张 [Card]）
 *
 * 父行 + 子项**共用一个背景块**，子项随开关**展开 / 收起**（[AnimatedVisibility]），
 * 不再像旧的「总开关一张卡、子项另一张卡」那样分层堆叠：
 *
 * ```
 * depth0  组标题行（总开关 = masterOf）
 *   │ 仅当总开关开
 * depth1  「阴影同步」          ← 仅圆角组；绑定 AppState.cardPressShadowSync
 * depth1  「板块同步」          ← syncOf
 *   ├ 板块同步关 → 展开 3 个板块行（depth1）：
 *   │   仅圆角组：板块行开 → depth2「四角同步」→ 四角同步关 → depth3 四角
 *   └ 板块同步开且圆角组 → depth1「四角同步」(summary「3 个板块共用」)
 *                         → 四角同步关 → depth2 共用四角
 * ```
 *
 * 缩进靠 [INDENT_L1] / [INDENT_L2] / [INDENT_L3] 加大 `insideMargin.start` 表达层级，
 * 不引入分隔线，与 [ImageScaleSettingsScreen] 的观感保持一致。
 */
@Composable
fun CardSettingsScreen() {
    val navigator = LocalNavigator.current
    val appState = LocalAppSettings.current
    val updateAppState = LocalUpdateAppSettings.current
    val scrollBehavior = MiuixScrollBehavior()

    AppSubPageScaffold(
        title = "卡片设置",
        largeTitleCentered = true,
        scrollBehavior = scrollBehavior,
        navigationIcon = {
            IconButton(onClick = { navigator.pop() }) {
                Icon(
                    imageVector = MiuixIcons.Back,
                    contentDescription = "返回",
                    tint = MiuixTheme.colorScheme.onBackground,
                )
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + 12.dp,
            ),
        ) {
            item(key = "card-settings") {
                CARD_TOGGLE_GROUPS.forEach { group ->
                    SmallTitle(text = group.title)

                    // 每个设置组只有一张 Card：父行（总开关）+ 子项共用一个背景块。
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp),
                    ) {
                        // depth0：组标题行（总开关）
                        SwitchPreference(
                            checked = group.masterOf(appState),
                            onCheckedChange = { updateAppState(group.withMaster(appState, it)) },
                            title = group.title,
                            summary = group.summary,
                        )

                        // 仅当总开关打开时展开子项（整块随开关播折叠动画）
                        AnimatedVisibility(
                            visible = group.masterOf(appState),
                            enter = EXPAND_TRANSITION,
                            exit = COLLAPSE_TRANSITION,
                        ) {
                            Column {
                                // depth1：「阴影同步」仅圆角组有，位于总开关正下方、板块同步之上
                                if (group.withCorners) {
                                    SwitchPreference(
                                        checked = appState.cardPressShadowSync,
                                        onCheckedChange = {
                                            updateAppState(appState.copy(cardPressShadowSync = it))
                                        },
                                        title = "阴影同步",
                                        summary = "按下的阴影轮廓跟卡片圆角一致；关 = 直角",
                                        insideMargin = INDENT_L1,
                                    )
                                }

                                // depth1：板块同步（决定 3 个板块是否共用同一份设置）
                                SwitchPreference(
                                    checked = group.syncOf(appState),
                                    onCheckedChange = { updateAppState(group.withSync(appState, it)) },
                                    title = "板块同步",
                                    summary = "开启后 3 个板块共用同一份设置",
                                    insideMargin = INDENT_L1,
                                )

                                // 板块同步关 → 展开 3 个板块行
                                AnimatedVisibility(
                                    visible = !group.syncOf(appState),
                                    enter = EXPAND_TRANSITION,
                                    exit = COLLAPSE_TRANSITION,
                                ) {
                                    Column {
                                        CardSection.entries.forEach { section ->
                                            // depth1：板块行（背景 / 圆角 / 胶囊）
                                            SwitchPreference(
                                                checked = group.effectiveChild(appState, section),
                                                onCheckedChange = {
                                                    updateAppState(group.withChild(appState, section, it))
                                                },
                                                title = "${section.label} ${group.childSuffix}",
                                                insideMargin = INDENT_L1,
                                            )

                                            if (group.withCorners) {
                                                // 该板块行打开 → 展开「四角同步」+ 四角
                                                AnimatedVisibility(
                                                    visible = group.effectiveChild(appState, section),
                                                    enter = EXPAND_TRANSITION,
                                                    exit = COLLAPSE_TRANSITION,
                                                ) {
                                                    Column {
                                                        // depth2：该板块「四角同步」
                                                        SwitchPreference(
                                                            checked = group.cornerSyncOf(appState, section),
                                                            onCheckedChange = {
                                                                updateAppState(
                                                                    group.withCornerSync(appState, section, it),
                                                                )
                                                            },
                                                            title = "四角同步",
                                                            insideMargin = INDENT_L2,
                                                        )

                                                        // depth3：四角（该板块「四角同步」关时才显示）
                                                        AnimatedVisibility(
                                                            visible = !group.cornerSyncOf(appState, section),
                                                            enter = EXPAND_TRANSITION,
                                                            exit = COLLAPSE_TRANSITION,
                                                        ) {
                                                            Column {
                                                                CardCorner.entries.forEach { corner ->
                                                                    SwitchPreference(
                                                                        checked = group.effectiveCorner(
                                                                            appState,
                                                                            section,
                                                                            corner,
                                                                        ),
                                                                        onCheckedChange = {
                                                                            updateAppState(
                                                                                group.withCorner(
                                                                                    appState,
                                                                                    section,
                                                                                    corner,
                                                                                    it,
                                                                                ),
                                                                            )
                                                                        },
                                                                        title = corner.label,
                                                                        insideMargin = INDENT_L3,
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // 板块同步开且圆角组 → 共用模式
                                if (group.withCorners) {
                                    AnimatedVisibility(
                                        visible = group.syncOf(appState),
                                        enter = EXPAND_TRANSITION,
                                        exit = COLLAPSE_TRANSITION,
                                    ) {
                                        Column {
                                            // depth1：共用「四角同步」（一次写 3 个板块）
                                            SwitchPreference(
                                                checked = group.sharedCornerSyncOf(appState),
                                                onCheckedChange = {
                                                    updateAppState(group.withSharedCornerSync(appState, it))
                                                },
                                                title = "四角同步",
                                                summary = "3 个板块共用",
                                                insideMargin = INDENT_L1,
                                            )

                                            // depth2：共用四角（共用「四角同步」关时才显示）
                                            AnimatedVisibility(
                                                visible = !group.sharedCornerSyncOf(appState),
                                                enter = EXPAND_TRANSITION,
                                                exit = COLLAPSE_TRANSITION,
                                            ) {
                                                Column {
                                                    CardCorner.entries.forEach { corner ->
                                                        SwitchPreference(
                                                            checked = group.sharedCornerOf(appState, corner),
                                                            onCheckedChange = {
                                                                updateAppState(
                                                                    group.withSharedCorner(appState, corner, it),
                                                                )
                                                            },
                                                            title = corner.label,
                                                            insideMargin = INDENT_L2,
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 展开动画：竖向撑开 + 淡入，时长 300ms。 */
private val EXPAND_TRANSITION: EnterTransition =
    expandVertically(
        animationSpec = tween(durationMillis = 300, easing = DecelerateEasing(1.5f)),
    ) + fadeIn(
        animationSpec = tween(durationMillis = 300, easing = DecelerateEasing(1.5f)),
    )

/** 收起动画：竖向收缩 + 淡出，时长 250ms。 */
private val COLLAPSE_TRANSITION: ExitTransition =
    shrinkVertically(
        animationSpec = tween(durationMillis = 250, easing = DecelerateEasing(1.5f)),
    ) + fadeOut(
        animationSpec = tween(durationMillis = 250, easing = DecelerateEasing(1.5f)),
    )

/**
 * 一级缩进（板块行 / 板块同步 / 阴影同步）。miuix
 * [top.yukonga.miuix.kmp.basic.BasicComponentDefaults.InsideMargin] 默认是 `PaddingValues(16.dp)`，
 * 这里把 start 加到 32.dp 表示比总开关低一层。
 */
private val INDENT_L1 = PaddingValues(start = 32.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)

/** 二级缩进（「四角同步」/ 共用四角行），比 [INDENT_L1] 再深一层。 */
private val INDENT_L2 = PaddingValues(start = 48.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)

/** 三级缩进（圆角组的四角开关），比 [INDENT_L2] 再深一层。 */
private val INDENT_L3 = PaddingValues(start = 64.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)
