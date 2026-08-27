package com.harvesttown.encyclopedia.ui.nav

import androidx.compose.runtime.staticCompositionLocalOf
import com.harvesttown.encyclopedia.data.repository.DataRepository
import com.harvesttown.encyclopedia.data.repository.SpriteRepository
import top.yukonga.miuix.kmp.nav.core.NavBackStack
import top.yukonga.miuix.kmp.nav.core.NavKey

/**
 * 导航助手：持有 miuix-nav 的返回栈 [NavBackStack]，提供 push/pop/replace/popUntil 等操作。
 *
 * push 采用幂等策略：返回栈中已存在相同 key 时不再重复入栈，避免连续双击触发重复内容。
 */
class Navigator(val backStack: NavBackStack) {
    /** 入栈；若栈中已存在相同 key 则跳过。 */
    fun push(key: NavKey) {
        if (key !in backStack) backStack.add(key)
    }

    /** 替换栈顶；栈空时改为入栈。 */
    fun replace(key: NavKey) {
        if (backStack.isNotEmpty()) {
            backStack[backStack.lastIndex] = key
        } else {
            backStack.add(key)
        }
    }

    /** 出栈（保留栈底）。 */
    fun pop() {
        if (backStack.size > 1) backStack.removeLastOrNull()
    }

    /** 出栈直到满足 [predicate]。 */
    fun popUntil(predicate: (NavKey) -> Boolean) {
        while (backStack.size > 1 && !predicate(backStack.last())) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    fun current(): NavKey? = backStack.lastOrNull()

    fun backStackSize(): Int = backStack.size
}

/** 当前导航器（各页面通过它进行路由跳转）。 */
val LocalNavigator = staticCompositionLocalOf<Navigator> { error("No navigator found!") }

/** 全局数据仓储（物品 / 配方 / NPC / 日程）。 */
val LocalDataRepository = staticCompositionLocalOf<DataRepository> { error("No DataRepository provided!") }

/** 全局切片仓储（懒切片 + 缓存的图标 / 立绘 / 星级图）。 */
val LocalSpriteRepository = staticCompositionLocalOf<SpriteRepository> { error("No SpriteRepository provided!") }
