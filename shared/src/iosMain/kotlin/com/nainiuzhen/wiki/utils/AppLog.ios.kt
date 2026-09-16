@file:OptIn(ExperimentalForeignApi::class)

package com.nainiuzhen.wiki.utils

import kotlin.concurrent.Volatile
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fputs

/**
 * iOS 诊断日志落地：追加写 `Documents/app_log.txt`（Filza / 爱思助手可直接取）。
 *
 * **性能关键**：日志可能发生在滚动/动画中的主线程。若每条日志都同步 fopen/fclose
 * （v42 之前的实现），每行就是一次主线程磁盘 I/O，是卡顿来源之一。现改为：
 * 行先入无界 Channel（调用方只付一次入队成本，永不挂起），由后台协程批量落盘
 * （首行后最多等 0.8s 凑批，把同帧日志风暴合并为一次打开/写入/关闭）。
 * 诊断页的即时查看走进程内环形缓冲（AppLog.readLog），不受落盘延迟影响。
 *
 * 只用 POSIX stdio + `NSSearchPathForDirectoriesInDomains`（后者在本工程已编译验证），
 * 规避 cinterop 里 Foundation 各类工厂方法的导入差异。
 * 全程 try/catch：**日志失败绝不能影响启动**。
 */
private const val LOG_FILE = "app_log.txt"

private val documentsDir: String? by lazy {
    try {
        (NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
            .firstOrNull() as? String)
    } catch (_: Throwable) {
        null
    }
}

/** 待落盘行通道（无界，trySend 永不挂起/丢弃）。 */
private val logChannel = Channel<String>(Channel.UNLIMITED)

@Volatile
private var sessionMarked = false

/**
 * 批量落盘协程：单消费者，FIFO 保序；首行到达后最多再收 0.8s 合并成一批。
 * 写入失败静默丢弃（内存环形缓冲仍在，页内「诊断日志」可看）。
 */
private val flusherStarted: Boolean by lazy {
    CoroutineScope(Dispatchers.Default).launch {
        val batch = ArrayList<String>(32)
        while (true) {
            batch.clear()
            batch.add(logChannel.receive()) // 挂起等首行
            while (true) {
                val more = withTimeoutOrNull(800) { logChannel.receive() } ?: break
                batch.add(more)
            }
            writeBatch(batch)
        }
    }
    true
}

private fun writeBatch(batch: List<String>) {
    if (batch.isEmpty()) return
    try {
        val dir = documentsDir ?: return
        val fp = fopen("$dir/$LOG_FILE", "a") ?: return
        try {
            for (line in batch) fputs("$line\n", fp)
        } finally {
            fclose(fp)
        }
    } catch (_: Throwable) {
        // 兜底：日志不可写时静默放弃
    }
}

/**
 * iOS 诊断日志落地入口。
 *
 * ⚠️ 不要在这里加 NSLog：os_log 要求格式串为编译期常量，K/N 传运行期字符串
 * 会在 CFLogvEx3 → os_log_with_args 里触发 EXC_BREAKPOINT（#18 版实测秒闪退）。
 *
 * @param line 已含级别与时间戳的一整行（见 [AppLog]）。
 */
internal actual fun platformLog(line: String) {
    try {
        flusherStarted // 首次调用时启动落盘协程
        if (!sessionMarked) {
            sessionMarked = true
            logChannel.trySend("==== 新会话 ====")
        }
        logChannel.trySend(line)
    } catch (_: Throwable) {
        // 兜底：日志不可写时静默放弃
    }
}
