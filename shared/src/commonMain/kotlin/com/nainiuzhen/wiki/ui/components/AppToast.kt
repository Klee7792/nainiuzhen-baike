package com.nainiuzhen.wiki.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.nainiuzhen.wiki.utils.ToastBridge
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 短 Toast 停留时长（近似系统 [android.widget.Toast.LENGTH_SHORT]）。 */
private const val TOAST_DURATION_MS = 2000L

/**
 * 跟随 App 主题（深浅色 / Monet）的 Toast 宿主。
 *
 * 置于组合树根部（[AppTheme] 之内），负责登记 [ToastBridge.sink] 并绘制最新一条文案。
 * 文案颜色取自 [MiuixTheme.colorScheme]，因此深色模式下为深底浅字、浅色模式下为浅底深字，
 * 与系统 Toast 永远白底不同。连续触发时重置计时、复用同一气泡。
 */
@Composable
fun AppToastHost(modifier: Modifier = Modifier) {
    var message by remember { mutableStateOf<String?>(null) }
    // token 自增用于「重新触发」自动消失计时（连续弹 toast 时重新计 2s）。
    var token by remember { mutableIntStateOf(0) }

    DisposableEffect(Unit) {
        val sink: (String) -> Unit = { msg ->
            message = msg
            token++
        }
        ToastBridge.sink = sink
        onDispose {
            if (ToastBridge.sink === sink) ToastBridge.sink = null
        }
    }

    LaunchedEffect(token) {
        if (message != null) {
            delay(TOAST_DURATION_MS)
            message = null
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedVisibility(
            visible = message != null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .padding(bottom = 96.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MiuixTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 18.dp, vertical = 11.dp),
            ) {
                Text(
                    text = message ?: "",
                    color = MiuixTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
