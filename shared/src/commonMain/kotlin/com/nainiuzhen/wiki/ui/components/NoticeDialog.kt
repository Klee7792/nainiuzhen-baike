package com.nainiuzhen.wiki.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.nainiuzhen.wiki.utils.LocalAppSettings
import com.nainiuzhen.wiki.utils.LocalUpdateAppSettings
import com.nainiuzhen.wiki.utils.NOTICE_TEXT
import com.nainiuzhen.wiki.utils.NOTICE_TITLE
import com.nainiuzhen.wiki.utils.NOTICE_VERSION
import com.nainiuzhen.wiki.utils.exitApplication
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 首次启动「使用须知」强制弹窗（法律免责用途）。
 *
 * 正文直接复用 [NOTICE_TEXT]（全应用唯一文案来源，见 `utils/Notice.kt`），
 * 逐段渲染、段间空 8dp；底部「不同意并退出 / 同意并继续」两个等宽按钮。
 *
 * ⚠️ **本弹窗必须留下「用户已知悉」的痕迹**：`onDismissRequest` 传空 lambda `{ }`，
 * 点遮罩与按返回键都**不能**关闭，只有点「同意并继续」才会写回
 * `agreedNoticeVersion`。这是本功能的法律落点，**不要**把它改成 `onAgree` / `onExit` 之类。
 *
 * @param show 是否显示。由 [NoticeGate] 持有，同意后转 false 播完退场动画。
 * @param onAgree 点「同意并继续」回调。
 * @param onExit 点「不同意并退出」回调。
 * @param onDismissFinished 退场动画完全结束后的回调（[NoticeGate] 用它回收整棵门禁）。
 */
@Composable
fun NoticeDialog(
    show: Boolean,
    onAgree: () -> Unit,
    onExit: () -> Unit,
    onDismissFinished: () -> Unit = {},
) {
    OverlayDialog(
        show = show,
        title = NOTICE_TITLE,
        // 固定底部弹出式（与移动端其余弹窗一致），不随窗口尺寸切到居中缩放。
        largeScreen = false,
        // ⚠️ 空 lambda：点遮罩 / 按返回键都关不掉，必须由用户点「同意并继续」留痕。
        // 这是「使用须知」强制确认的法律意义所在，请勿改为 onAgree / onExit。
        onDismissRequest = { },
        onDismissFinished = onDismissFinished,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 正文：段落较多时高度受限并可滚动，避免长文案把按钮顶出屏幕。
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                NOTICE_TEXT.forEachIndexed { i, para ->
                    Text(
                        text = para,
                        fontSize = MiuixTheme.textStyles.body2.fontSize,
                        // 正文行距 1.6×字号：免责声明较长，行距偏紧会难读。
                        lineHeight = MiuixTheme.textStyles.body2.fontSize * 1.6f,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                    if (i != NOTICE_TEXT.lastIndex) {
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            // 底部按钮行：两键各 weight(1f) 等宽（同 NpcDetailScreen 的按钮区写法）。
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TextButton(
                    text = "不同意并退出",
                    onClick = onExit,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    text = "同意并继续",
                    onClick = onAgree,
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * 「使用须知」门禁：在主界面就绪后挂载，未同意当前 [NOTICE_VERSION] 时强制弹窗。
 *
 * `show` 用**无 key 的 remember** 只在首次组合求值一次：点「同意」后 `show` 转 false，
 * 弹窗能播完退场动画再消失。若改成每次按 `agreedNoticeVersion` 直接判断，
 * 同意瞬间版本号即达标、`show` 立刻变 false，弹窗会「瞬间消失、无动画」。
 */
@Composable
fun NoticeGate() {
    val appState = LocalAppSettings.current
    val updateAppSettings = LocalUpdateAppSettings.current
    // 两个都只在首次组合求值：同意后 show 转 false 播完退场动画，
    // 动画结束由 OverlayDialog 的 onDismissFinished 把整棵门禁（含它自带的
    // Scaffold 作用域）移出组合，避免长期挂着一个全屏透明 Scaffold。
    var show by remember { mutableStateOf(appState.agreedNoticeVersion < NOTICE_VERSION) }
    var composed by remember { mutableStateOf(show) }
    if (!composed) return

    // ⚠️ 为什么门禁必须自带一个 Scaffold（勿改成裸挂 OverlayDialog）：
    // miuix 的弹窗真正由 `MiuixPopupHost()` 渲染（MiuixPopupUtils.kt:544-556），
    // 而它只是 `Scaffold` 的默认 `popupHost`（Scaffold.kt:88）；`DialogLayout` 自己
    // 只负责把状态注册进 `LocalDialogStates`（MiuixPopupUtils.kt:184-231）。
    // 因此在没有 Scaffold 祖先的地方挂 OverlayDialog，状态会注册进默认空列表
    // （MiuixPopupUtils.kt:596）、**没有任何渲染者** ⇒ 弹窗静默不显示，「不同意」也点不到。
    // App.kt 全文件没有 Scaffold，而这里又必须是兄弟作用域而非祖先（祖先会改变
    // MainScreen 内既有弹窗的渲染目标，破坏分栏下「遮罩只盖右栏」的既定行为），
    // 所以自建一个透明、无内容、只用于承载本弹窗的 root Scaffold。
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
    ) {
        // 显式吞掉全部触摸：不依赖 OverlayDialog 遮罩是否消费点击，
        // 防止用户点穿门禁去操作下面的应用。
        if (show) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) { detectTapGestures { } },
            )
        }
        NoticeDialog(
            show = show,
            onAgree = {
                updateAppSettings(appState.copy(agreedNoticeVersion = NOTICE_VERSION))
                show = false
            },
            onExit = { exitApplication() },
            onDismissFinished = { composed = false },
        )
    }
}
