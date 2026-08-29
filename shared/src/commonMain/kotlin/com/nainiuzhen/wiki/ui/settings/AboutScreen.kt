// 奶牛镇百科 · 关于子页（v14 复刻 miuix demo AboutPage）
//
// 布局完全对齐 demo 截图：
//   - 全屏 OS3 动态渐变背景；
//   - 左上角返回箭头；
//   - 居中应用图标 + 大标题「奶牛镇百科」+ 版本号 vX.Y.Z (code)；
//   - 两个圆角卡片：
//       查看源码 / 加入群组
//       开源协议 / 第三方开源协议
//   - 所有条目点击弹出 Toast「还没做」。

package com.nainiuzhen.wiki.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nainiuzhen.wiki.ui.nav.LocalNavigator
import com.nainiuzhen.wiki.ui.settings.about.BgEffectBackground
import com.nainiuzhen.wiki.utils.APP_VERSION_CODE
import com.nainiuzhen.wiki.utils.APP_VERSION_NAME
import com.nainiuzhen.wiki.utils.LocalAppSettings
import com.nainiuzhen.wiki.utils.showToast
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AboutScreen() {
    val navigator = LocalNavigator.current
    val appState = LocalAppSettings.current

    val systemDark = isSystemInDarkTheme()
    val isDark = when (appState.colorMode) {
        1 -> true
        2 -> false
        else -> systemDark
    }

    val surface = MiuixTheme.colorScheme.surface
    val backdrop = rememberLayerBackdrop()
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        // OS3 动态背景（与 BgEffectBackground 内部 layerBackdrop 配合）
        BgEffectBackground(
            dynamicBackground = true,
            isDark = isDark,
            surface = surface,
            modifier = Modifier.fillMaxSize(),
            bgModifier = Modifier.layerBackdrop(backdrop),
        ) {}

        // 返回按钮
        IconButton(
            onClick = { navigator.pop() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 4.dp, top = 4.dp),
        ) {
            Icon(
                imageVector = MiuixIcons.Back,
                contentDescription = "返回",
                tint = Color.White,
            )
        }

        // 居中内容：图标 / 标题 / 版本 / 卡片
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(top = 100.dp, bottom = 24.dp)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            // 应用图标占位（圆角矩形 + 主题色文字）
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "奶",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4A90E2),
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = "奶牛镇百科",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "$APP_VERSION_NAME ($APP_VERSION_CODE)",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(48.dp))

            AboutCard {
                AboutRow(
                    title = "查看源码",
                    summary = "GitHub",
                    onClick = { showToast("还没做") },
                )
                HorizontalDivider(
                    color = MiuixTheme.colorScheme.dividerLine,
                    thickness = 0.5.dp,
                )
                AboutRow(
                    title = "加入群组",
                    summary = "Telegram",
                    onClick = { showToast("还没做") },
                )
            }

            Spacer(Modifier.height(14.dp))

            AboutCard {
                AboutRow(
                    title = "开源协议",
                    summary = "Apache-2.0",
                    onClick = { showToast("还没做") },
                )
                HorizontalDivider(
                    color = MiuixTheme.colorScheme.dividerLine,
                    thickness = 0.5.dp,
                )
                AboutRow(
                    title = "第三方开源协议",
                    summary = null,
                    onClick = { showToast("还没做") },
                )
            }
        }
    }
}

@Composable
private fun AboutCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun AboutRow(
    title: String,
    summary: String?,
    onClick: () -> Unit,
) {
    BasicComponent(
        title = title,
        summary = null,
        endActions = {
            if (summary != null) {
                Text(
                    text = summary,
                    fontSize = MiuixTheme.textStyles.body2.fontSize,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(end = 2.dp),
                )
            }
            Icon(
                imageVector = MiuixIcons.Basic.ArrowRight,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        },
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    )
}
