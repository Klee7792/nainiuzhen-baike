package com.nainiuzhen.wiki.ui.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import top.yukonga.miuix.kmp.utils.overScrollVertical
import androidx.compose.ui.unit.dp
import com.nainiuzhen.wiki.ui.components.AppSubPageScaffold
import com.nainiuzhen.wiki.ui.nav.LocalNavigator
import com.nainiuzhen.wiki.utils.LocalAppSettings
import com.nainiuzhen.wiki.utils.LocalUpdateAppSettings
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.preference.WindowSpinnerPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt

/**
 * 素材缩放设置子页（从设置页「素材缩放设置」进入）。
 *
 * 5 个独立滑块（#22 复盘，弹窗细分为 3 项）：
 * - 「列表与主页」
 *   - 卡片素材 [com.nainiuzhen.wiki.utils.AppState.cardImageScale]（默认 5，最大 8）：物品 / 配方卡片内图片。
 *   - 主页左侧素材 [com.nainiuzhen.wiki.utils.AppState.homeImageScale]（默认 8，最大 10）：主页物品 / 配方入口卡片图。
 * - 「详情弹窗」
 *   - 弹窗本体素材 [com.nainiuzhen.wiki.utils.AppState.dialogBodyImageScale]（默认 6，最大 8）：物品 / 配方详情头部素材。
 *   - 弹窗配方素材 [com.nainiuzhen.wiki.utils.AppState.dialogRecipeImageScale]（默认 6，最大 8）：配方原料 / 产物。
 *   - 弹窗喜恶素材 [com.nainiuzhen.wiki.utils.AppState.dialogFavHateImageScale]（默认 6，最大 8）：NPC 最爱 / 喜欢 / 讨厌。
 * - 「卡片文字」（绝对值字号，步长跟随本页「步长」，默认 10）
 *   - 物品大全卡片文字 [com.nainiuzhen.wiki.utils.AppState.itemCardTextSizeSp]（默认 10）：物品卡片名称字号，单位 sp，范围 5 ~ 11。
 *   - 配方查询卡片文字 [com.nainiuzhen.wiki.utils.AppState.recipeCardTextSizeSp]（默认 10）：配方卡片名称字号，单位 sp，范围 5 ~ 11。
 *
 * 滑块以 0.1 为步进；拖动即时生效（经 [LocalAppSettings]/[LocalUpdateAppSettings] 落盘），
 * 对应区域素材随 [com.nainiuzhen.wiki.ui.components.SpriteScaleContext] 联动刷新。
 */
@Composable
fun ImageScaleSettingsScreen() {
    val navigator = LocalNavigator.current
    val appState = LocalAppSettings.current
    val updateAppState = LocalUpdateAppSettings.current
    val scrollBehavior = MiuixScrollBehavior()

    AppSubPageScaffold(
        title = "素材缩放设置",
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
            item(key = "list-home") {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                ) {
                    WindowSpinnerPreference(
                        items = STEP_OPTIONS,
                        selectedIndex = stepIndex(appState.scaleStep),
                        title = "步长",
                        summary = "滑块每次拖动增加的量",
                        onSelectedIndexChange = { idx ->
                            updateAppState(appState.copy(scaleStep = STEP_VALUES[idx]))
                        },
                    )
                }

                SmallTitle(text = "卡片文字")
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                ) {
                    ScaleSlider(
                        value = appState.itemCardTextSizeSp,
                        onValueChange = { updateAppState(appState.copy(itemCardTextSizeSp = it)) },
                        title = "物品大全卡片文字",
                        summary = "物品卡片名称字号（5 ~ 11 sp，默认 10）",
                        min = 5f,
                        max = 11f,
                        step = appState.scaleStep,
                        suffix = "sp",
                    )
                    ScaleSlider(
                        value = appState.recipeCardTextSizeSp,
                        onValueChange = { updateAppState(appState.copy(recipeCardTextSizeSp = it)) },
                        title = "配方查询卡片文字",
                        summary = "配方卡片名称字号（5 ~ 11 sp，默认 10）",
                        min = 5f,
                        max = 11f,
                        step = appState.scaleStep,
                        suffix = "sp",
                    )
                }

                SmallTitle(text = "列表与主页")
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                ) {
                    ScaleSlider(
                        value = appState.cardImageScale,
                        onValueChange = { updateAppState(appState.copy(cardImageScale = it)) },
                        title = "卡片素材",
                        summary = "物品 / 配方卡片内图片",
                        max = 8f,
                        step = appState.scaleStep,
                    )
                    ScaleSlider(
                        value = appState.homeImageScale,
                        onValueChange = { updateAppState(appState.copy(homeImageScale = it)) },
                        title = "主页左侧素材",
                        summary = "主页物品 / 配方入口卡片图",
                        max = 10f,
                        step = appState.scaleStep,
                    )
                }

                SmallTitle(text = "详情弹窗")
                Card(
                    modifier = Modifier.padding(horizontal = 12.dp),
                ) {
                    ScaleSlider(
                        value = appState.dialogBodyImageScale,
                        onValueChange = { updateAppState(appState.copy(dialogBodyImageScale = it)) },
                        title = "弹窗本体素材",
                        summary = "物品 / 配方详情头部素材",
                        max = 8f,
                        step = appState.scaleStep,
                    )
                    ScaleSlider(
                        value = appState.dialogRecipeImageScale,
                        onValueChange = { updateAppState(appState.copy(dialogRecipeImageScale = it)) },
                        title = "弹窗配方素材",
                        summary = "配方原料 / 产物",
                        max = 8f,
                        step = appState.scaleStep,
                    )
                    ScaleSlider(
                        value = appState.dialogFavHateImageScale,
                        onValueChange = { updateAppState(appState.copy(dialogFavHateImageScale = it)) },
                        title = "弹窗喜恶素材",
                        summary = "NPC 最爱 / 喜欢 / 讨厌",
                        max = 8f,
                        step = appState.scaleStep,
                    )
                }
            }
        }
    }
}

private val STEP_VALUES = listOf(0.1f, 0.5f, 1f)
private val STEP_OPTIONS = STEP_VALUES.map { DropdownItem(text = "%.1f".format(it)) }

/** 根据当前步长 Float 取其在 STEP_VALUES 中的下标（带容差，避免 0.1f 浮点漂移）。 */
private fun stepIndex(step: Float): Int {
    val idx = STEP_VALUES.indexOfFirst { kotlin.math.abs(it - step) < 1e-4f }
    return if (idx >= 0) idx else 0
}

/**
 * 数值滑块：范围 [min, max]，步进由 [step] 决定（steps = ((max-min)/step).roundToInt() - 1），
 * 右侧显示一位小数值 + [suffix]。用 roundToInt 防 0.1 浮点除法漂移；steps 下限 0 防御
 * 极端组合（如区间小于步长时只能取两端点）。
 *
 * [min] 默认 1f / [suffix] 默认 "×" ⇒ 5 个既有素材滑块行为不变；
 * 「卡片文字」组用 min=5f、max=11f、suffix="sp"（绝对值字号）。
 */
@Composable
private fun ScaleSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    title: String,
    summary: String,
    max: Float,
    step: Float,
    min: Float = 1f,
    suffix: String = "×",
) {
    val steps = (((max - min) / step).roundToInt() - 1).coerceAtLeast(0)
    SliderPreference(
        value = value,
        onValueChange = onValueChange,
        title = title,
        summary = summary,
        valueRange = min..max,
        steps = steps,
        valueText = "%.1f".format(value) + suffix,
    )
}
