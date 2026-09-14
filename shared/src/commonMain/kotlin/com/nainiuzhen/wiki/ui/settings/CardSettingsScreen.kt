package com.nainiuzhen.wiki.ui.settings

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
 * 三组装饰开关（卡片背景 / 卡片圆角 / 文字胶囊）结构完全一致，统一由
 * [com.nainiuzhen.wiki.utils.CARD_TOGGLE_GROUPS] 描述、本页循环渲染，避免三份重复 UI：
 *
 * - 每组：`SmallTitle` 组标题 + 一张「总开关」Card（`group.masterOf`）。
 * - 总开关开时，再渲染一张子项 Card：
 *   - 每个 [CardSection] 一行板块开关，`checked` 用 `group.effectiveChild`（**跟随后的显示值**，
 *     非存的值），同步开时置灰（`enabled = !group.syncOf`）。
 *   - 圆角组（`withCorners`）在该板块开关打开后，继续展开四个 [CardCorner] 开关
 *     （`checked` 用 `group.effectiveCorner`）+ 一行「四角同步」。
 *   - 组级「同步」行：开启后各板块跟随总开关，不能单独关闭。
 *   - 圆角组在「同步」下面再补一行「按下阴影圆角同步」（绑定 `AppState.cardPressShadowSync`）。
 *
 * 缩进靠 [INDENT_L1] / [INDENT_L2] 加大 `insideMargin.start` 表达层级，不引入分隔线，
 * 与 [ImageScaleSettingsScreen] 的观感保持一致。
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
                    // 1) 组标题
                    SmallTitle(text = group.title)

                    // 2) 总开关
                    Card(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 12.dp),
                    ) {
                        SwitchPreference(
                            checked = group.masterOf(appState),
                            onCheckedChange = { updateAppState(group.withMaster(appState, it)) },
                            title = group.title,
                            summary = group.summary,
                        )
                    }

                    // 3) 仅当总开关打开时，才渲染子项 Card
                    if (group.masterOf(appState)) {
                        Card(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .padding(bottom = 12.dp),
                        ) {
                            CardSection.entries.forEach { section ->
                                // 板块开关：显示「跟随后的值」，同步开时置灰
                                SwitchPreference(
                                    checked = group.effectiveChild(appState, section),
                                    onCheckedChange = {
                                        updateAppState(group.withChild(appState, section, it))
                                    },
                                    title = "${section.label} ${group.childSuffix}",
                                    enabled = !group.syncOf(appState),
                                    insideMargin = INDENT_L1,
                                )

                                // 圆角组：板块打开后展开四角 + 四角同步
                                if (group.withCorners && group.effectiveChild(appState, section)) {
                                    CardCorner.entries.forEach { corner ->
                                        SwitchPreference(
                                            checked = group.effectiveCorner(appState, section, corner),
                                            onCheckedChange = {
                                                updateAppState(
                                                    group.withCorner(appState, section, corner, it),
                                                )
                                            },
                                            title = corner.label,
                                            enabled = !group.cornerSyncOf(appState, section),
                                            insideMargin = INDENT_L2,
                                        )
                                    }
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
                                }
                            }

                            // 组级「同步」行
                            SwitchPreference(
                                checked = group.syncOf(appState),
                                onCheckedChange = { updateAppState(group.withSync(appState, it)) },
                                title = "同步",
                                summary = "开启后各板块跟随总开关，不能单独关闭",
                            )

                            // 圆角组：在「同步」下面、同一层级追加「按下阴影圆角同步」
                            if (group.withCorners) {
                                SwitchPreference(
                                    checked = appState.cardPressShadowSync,
                                    onCheckedChange = {
                                        updateAppState(appState.copy(cardPressShadowSync = it))
                                    },
                                    title = "按下阴影圆角同步",
                                    summary = "按下的阴影轮廓跟卡片圆角一致；关 = 直角",
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 一级缩进（板块开关行）。miuix [top.yukonga.miuix.kmp.basic.BasicComponentDefaults.InsideMargin]
 * 默认是 `PaddingValues(16.dp)`，这里把 start 加到 32.dp 表示比总开关低一层。
 */
private val INDENT_L1 = PaddingValues(start = 32.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)

/** 二级缩进（四角开关 / 四角同步行），比 [INDENT_L1] 再深一层。 */
private val INDENT_L2 = PaddingValues(start = 48.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)
