package com.nainiuzhen.wiki.utils

import kotlinx.coroutines.CoroutineDispatcher

/**
 * IO 密集型调度器（跨平台）。
 *
 * `Dispatchers.IO` 在 Kotlin/Native 是 internal 的，commonMain 不能直接引用；
 * Android actual 用 Dispatchers.IO（行为与原实现完全一致），
 * iOS actual 用 Dispatchers.Default（K/N 无公共 IO 调度器，社区标准替代）。
 */
expect val IoDispatcher: CoroutineDispatcher
