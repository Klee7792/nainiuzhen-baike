// 第三方开源协议子页（参考 miuix demo LicensePage 结构搬入）。
// 顶栏返回箭头 → navigator.pop() 回到关于页；列表展示本应用依赖的第三方开源库。

package com.nainiuzhen.wiki.ui.settings.about

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.nainiuzhen.wiki.ui.components.AppSubPageScaffold
import com.nainiuzhen.wiki.ui.nav.LocalNavigator
import com.nainiuzhen.wiki.utils.showToast
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 第三方开源协议列表（静态搬运自 demo 的 aboutlibraries 思路；后续可接入真实
 * aboutlibraries.json 解析）。点击条目暂弹出 toast 占位。
 */
private data class OssLibrary(
    val name: String,
    val versionAndLicense: String,
)

private val ossLibraries = listOf(
    OssLibrary("miuix", "Compose Multiplatform UI · Apache-2.0"),
    OssLibrary("Kotlin", "JVM/JS/Native 语言 · Apache-2.0"),
    OssLibrary("kotlinx-coroutines", "1.x · Apache-2.0"),
    OssLibrary("kotlinx-serialization", "1.x · Apache-2.0"),
    OssLibrary("Compose Multiplatform", "UI 工具集 · Apache-2.0"),
    OssLibrary("AndroidX", "core / lifecycle / etc. · Apache-2.0"),
    OssLibrary("Android SDK", "平台运行时 · Apache-2.0"),
)

@Composable
fun LicenseScreen() {
    val navigator = LocalNavigator.current
    val scrollBehavior = MiuixScrollBehavior()

    AppSubPageScaffold(
        title = "第三方开源协议",
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
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxHeight()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                start = 12.dp,
                top = innerPadding.calculateTopPadding(),
                end = 12.dp,
                bottom = innerPadding.calculateBottomPadding() + 12.dp,
            ),
        ) {
            items(ossLibraries, key = { it.name }) { lib ->
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(top = 12.dp),
                ) {
                    ArrowPreference(
                        title = lib.name,
                        summary = lib.versionAndLicense,
                        onClick = { showToast("许可证详情暂未接入") },
                    )
                }
            }
        }
    }
}
