package com.nainiuzhen.wiki.ui.nav

import androidx.compose.runtime.staticCompositionLocalOf
import com.nainiuzhen.wiki.data.repository.DataRepository
import com.nainiuzhen.wiki.data.repository.SpriteRepository
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

    /**
     * **顶层跳转**：清空到栈底后入栈 —— 用于「左栏板块 / 设置项」这类**平级**入口。
     *
     * 为什么需要它（大屏分栏下暴露的真实缺陷）：
     * [push] 的幂等策略会**跳过栈中已有的 key**。而分栏时左栏常驻可见，用户可以连续点
     * 物品大全 → 配方查询 → NPC 资料，栈会累积成 `[Main, ItemList, RecipeList, NpcList, …]`；
     * 此时再点「物品大全」，因为 `ItemList` 仍留在栈中，`push` 被幂等跳过 ⇒ **界面毫无反应**。
     * 板块之间是**平级关系**（不是层级深入），所以正确语义是「换成它」，不是「叠在它上面」。
     *
     * 与 [push] 的分工：
     * - [openTopLevel]：左栏发起的平级跳转（主页三板块、设置 → 关于 / 图片倍率）。
     * - [push]：右栏子页内部的**层级深入**（NPC 详情 → 日程、关于 → 开源协议），这种才该叠加。
     *
     * 手机单栏下无副作用：主页可见时栈恒为 `[Main]`，清空到栈底是空操作，行为与 [push] 完全一致。
     *
     * @param key 目标板块路由。
     */
    fun openTopLevel(key: NavKey) {
        // 已经停在该板块上时保持原状：不要把 key 摘了重加，否则会重置页内的搜索/筛选/滚动状态。
        if (backStack.size == 2 && backStack[1] == key) return
        while (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
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
