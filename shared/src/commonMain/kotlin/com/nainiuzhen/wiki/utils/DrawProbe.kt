package com.nainiuzhen.wiki.utils

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.onSizeChanged

/**
 * 绘制期诊断探针（iOS 无 Mac 排障专用，随版本稳定后移除）。
 *
 * 背景：iOS 上出现过「加载页正常、进入主界面后整屏只剩背景色」的故障 ——
 * composition 没死（toast 都弹了）、无异常日志，问题只能出在**绘制/布局阶段**。
 * 本文件提供三个零成本（首帧后自动停用）的探针，挂在可疑节点上即可在
 * Documents/app_log.txt 里看到：
 *
 * - 节点实际尺寸（验证「尺寸不对」类问题，像素值 = dp × density）；
 * - 节点是否真的执行到了 drawContent()（没日志 = 没被绘制 / 尺寸为 0）；
 * - 子树绘制抛异常时的**原位捕获**（捕获后不再外抛：既保住日志，又能让同帧
 *   其余兄弟节点继续画出来 —— 这本身就是对「draw 阶段异常整帧作废」的防御）。
 */
/** 尺寸探针：节点尺寸变化时记一条日志（像素）。 */
fun Modifier.logSize(tag: String): Modifier = composed {
    onSizeChanged { size ->
        AppLog.i("size $tag = ${size.width}x${size.height}px")
    }
}

/**
 * 绘制成功探针：首次执行到 drawContent() 后记一条日志，之后不再记录。
 * 若日志里**没有**某节点的 `draw ✓`，说明它压根没画（尺寸 0 / 未布局 / 被跳过）。
 */
fun Modifier.logFirstDraw(tag: String): Modifier = composed {
    var logged by remember { mutableStateOf(false) }
    drawWithContent {
        drawContent()
        if (!logged) {
            logged = true
            AppLog.i("draw ✓ $tag")
        }
    }
}

/**
 * 绘制异常探针：捕获子树绘制阶段的异常并写日志（含堆栈），**不再外抛**。
 *
 * 用途：怀疑「某个子节点的 draw 抛异常导致整帧作废 / 后续兄弟节点全黑」时，
 * 把本探针挂在各子树根部 —— 哪个 tag 报了 `draw ✗` 就锁定了异常源头，
 * 同时由于异常被吞掉，同帧其余部分还能正常呈现。
 */
fun Modifier.logDrawError(tag: String): Modifier = composed {
    var logged by remember { mutableStateOf(false) }
    drawWithContent {
        try {
            drawContent()
            if (!logged) {
                logged = true
                AppLog.i("draw ✓ $tag")
            }
        } catch (t: Throwable) {
            if (!logged) {
                logged = true
                AppLog.e("draw ✗ $tag", t)
            } else {
                AppLog.e("draw ✗ $tag（重复）", t)
            }
        }
    }
}
