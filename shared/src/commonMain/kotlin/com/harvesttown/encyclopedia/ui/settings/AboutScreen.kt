package com.harvesttown.encyclopedia.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.harvesttown.encyclopedia.ui.nav.LocalNavigator
import com.harvesttown.encyclopedia.ui.nav.LocalSpriteRepository
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 关于子页（变更点 #4 / T8）：独立全屏页面，背景采用 OS3 紫 → 蓝 [Brush.verticalGradient] 渐变。
 * 由设置页「关于」[com.harvesttown.encyclopedia.ui.settings.SettingsContent] 经 [com.harvesttown.encyclopedia.ui.nav.Route.About] 进入。
 */
@Composable
fun AboutScreen() {
    val navigator = LocalNavigator.current
    val sprite = LocalSpriteRepository.current
    val scrollBehavior = MiuixScrollBehavior()
    val gradient = Brush.verticalGradient(listOf(Color(0xFF7C4DFF), Color(0xFF3482FF)))

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "关于",
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
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(innerPadding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "奶牛镇百科",
                    style = MiuixTheme.textStyles.title1,
                    color = Color.White,
                )
                Text(
                    text = "版本 v1.0.0 (${sprite.currentVersion()})",
                    style = MiuixTheme.textStyles.body1,
                    color = Color.White,
                )
                Text(
                    text = "奶牛镇物语全图鉴查询工具，支持物品、配方、NPC 资料与日程检索。",
                    style = MiuixTheme.textStyles.body2,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
        }
    }
}
