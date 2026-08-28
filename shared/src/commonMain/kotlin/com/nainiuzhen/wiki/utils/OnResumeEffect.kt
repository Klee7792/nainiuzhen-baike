package com.nainiuzhen.wiki.utils

import androidx.compose.runtime.Composable

/**
 * 在宿主 Activity 进入 RESUME（首次进入 / 从后台返回）时触发 [onResume]。
 * 用于主页「回到前台重新随机刷新」等场景（变更点 #34）。
 * 非 Android 端为空实现。
 */
@Composable
expect fun OnResumeEffect(onResume: () -> Unit)
