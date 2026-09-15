package com.nainiuzhen.wiki.ui.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nainiuzhen.wiki.data.source.PackDecoder
import com.nainiuzhen.wiki.ui.components.AppSubPageScaffold
import com.nainiuzhen.wiki.ui.nav.LocalDataRepository
import com.nainiuzhen.wiki.ui.nav.LocalNavigator
import com.nainiuzhen.wiki.ui.nav.LocalSpriteRepository
import com.nainiuzhen.wiki.utils.AppLog
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

/**
 * 诊断信息子页（设置页「诊断日志」进入）。
 *
 * 存在的意义：iOS 侧无 Mac、连不上 Xcode 调试器，一旦出现「素材没加载出来但也没报错」
 * 这类静默故障，唯一线索就是日志。本页把**数据概况**与**运行日志**直接铺在页面上，
 * 截屏即可反馈（同时也照旧落到 Documents/app_log.txt，可用 Filza 取出）。
 *
 * 数据概况里的每一项都对应一处加载环节，正常值参考：
 *   · 物品 ≈ 3900+ / 配方 ≈ 1000+ / NPC ≈ 117
 *   · 图集帧 ≈ 6565，已切片应与之相等（小于 = 有图集切片失败）
 *   · assets.pack 条目 = 打包进包的素材文件数（0 = 包没打进去或解析失败）
 */
@Composable
fun DiagnosticsScreen() {
    val navigator = LocalNavigator.current
    val data = LocalDataRepository.current
    val sprite = LocalSpriteRepository.current
    val scrollBehavior = MiuixScrollBehavior()
    // 日志快照：进入页面时读一次，点「刷新」再读一次（懒刷新，不做定时轮询）。
    var log by remember { mutableStateOf(AppLog.readLog()) }

    AppSubPageScaffold(
        title = "诊断日志",
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
            item(key = "summary") {
                SmallTitle(text = "数据概况")
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                ) {
                    BasicComponent(title = "物品", summary = "${data.items.size} 条")
                    BasicComponent(title = "配方", summary = "${data.recipes.size} 条")
                    BasicComponent(title = "NPC", summary = "${data.npcs.size} 条")
                    BasicComponent(title = "日程 NPC", summary = "${data.schedules.size} 个")
                    BasicComponent(
                        title = "图集帧",
                        summary = "${sprite.atlasFrameCount} 帧（已切片 ${sprite.loadedSpriteCount}）",
                    )
                    BasicComponent(
                        title = "assets.pack",
                        summary = "${PackDecoder.entryCount} 条目",
                    )
                }
            }
            item(key = "log") {
                SmallTitle(text = "运行日志（最近 ${log.lineSequence().count()} 行）")
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp),
                ) {
                    BasicComponent(
                        title = "刷新",
                        summary = "重新读取内存日志（同时已写入 Documents/app_log.txt）",
                        onClick = { log = AppLog.readLog() },
                    )
                    Text(
                        text = log.ifBlank { "（暂无日志）" },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Normal,
                        color = MiuixTheme.colorScheme.onBackground,
                    )
                }
            }
        }
    }
}
