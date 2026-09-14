package com.nainiuzhen.wiki.utils

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 「卡片外观设置」（v31）的求值与分组契约。
 *
 * ## 三组开关，同一套形状
 *
 * 卡片背景 / 卡片圆角 / 文字胶囊三组，结构完全一致：**总开关 + n 个板块 + 同步**。
 * 新增板块只需在 [CardSection] 加一项、并在 [AppState] 补对应字段，三组会自动把它纳入。
 *
 * ## 求值规则（唯一口径）
 *
 * ```
 * effective = 总开关 && (同步 || 板块自己的值)
 * ```
 *
 * - 总开关关 ⇒ 该装饰整体关闭（默认态，等于「关闭背景 / 直角 / 无胶囊」）。
 * - 总开关开 + 同步开 ⇒ n 个板块**全部跟随开启**，此时板块行不可单独关闭（界面上置灰）。
 * - 总开关开 + 同步关 ⇒ 每个板块按自己的值走，可以自定义。
 *
 * 圆角组在板块之下还有一层**套娃**：四个角各自一个开关 + 「四角同步」。
 * ```
 * 角生效 = 板块圆角开 && (四角同步 || 该角自己的值)
 * ```
 *
 * ## 为什么参数名这么长
 *
 * 三组开关的字段名是 `cardBg*` / `cardCorner*` / `cardCapsule*` 前缀，**故意写全**，
 * 因为 [AppState] 是一个 90+ 字段的扁平淡对象，靠 `copy()` 更新；名字短了在调用点看不出是哪一组。
 */

/** 卡片外观设置作用的「板块」（= 3 个图鉴子页面）。以后新增板块在此加一项即可。 */
enum class CardSection(val label: String) {
    Item("物品大全"),
    Recipe("配方查询"),
    Npc("NPC 资料"),
}

/** 四个角（顺序与设置页展示一致：左上 / 右上 / 左下 / 右下）。 */
enum class CardCorner(val label: String) {
    TopStart("左上角"),
    TopEnd("右上角"),
    BottomStart("左下角"),
    BottomEnd("右下角"),
}

/** 「圆角开」时的半径；关 = `0.dp`（直角）。 */
val CARD_CORNER_RADIUS: Dp = 16.dp

// ————————————————————————————————————————————————————————————
// 板块级求值
// ————————————————————————————————————————————————————————————

/** 该板块的「素材背景」是否显示（图片后面那层底色）。 */
fun AppState.cardBgEnabled(section: CardSection): Boolean =
    cardBgMaster && (cardBgSync || cardBgChild(section))

/** 该板块的「文字胶囊」是否显示（名称那行蓝底）。关 ⇒ 纯文字，无底色。 */
fun AppState.cardCapsuleEnabled(section: CardSection): Boolean =
    cardCapsuleMaster && (cardCapsuleSync || cardCapsuleChild(section))

/** 该板块的「圆角」总闸是否打开（打开才可能有非零圆角 / 按下阴影）。 */
fun AppState.cardCornerEnabled(section: CardSection): Boolean =
    cardCornerMaster && (cardCornerSync || cardCornerChild(section))

// ————————————————————————————————————————————————————————————
// 四角求值
// ————————————————————————————————————————————————————————————

/** 单个角的生效半径：板块总闸关 ⇒ 0；四角同步开 ⇒ 跟板块；否则看该角自己的值。 */
fun AppState.cardCornerRadiusOf(section: CardSection, corner: CardCorner): Dp {
    if (!cardCornerEnabled(section)) return 0.dp
    val on = if (cardCornerSync4(section)) true else cardCornerChildOf(section, corner)
    return if (on) CARD_CORNER_RADIUS else 0.dp
}

/** 该板块的实际圆角形状（四角可各不相同）。 */
fun AppState.cardShape(section: CardSection): CornerBasedShape = RoundedCornerShape(
    topStart = cardCornerRadiusOf(section, CardCorner.TopStart),
    topEnd = cardCornerRadiusOf(section, CardCorner.TopEnd),
    bottomEnd = cardCornerRadiusOf(section, CardCorner.BottomEnd),
    bottomStart = cardCornerRadiusOf(section, CardCorner.BottomStart),
)

/**
 * 按下阴影的轮廓形状。
 *
 * 「按下阴影圆角同步」开 ⇒ 跟卡片实际圆角一致；关 ⇒ 一律直角。
 * 圆角组总开关关时卡片本来就是直角，此时两者等价 —— 所以该开关只在圆角开启后才有意义。
 */
fun AppState.cardPressShadowShape(section: CardSection): Shape =
    if (cardPressShadowSync) cardShape(section) else RectangleShape

/** 是否渲染按下阴影：只有圆角组总开关打开时才渲染，保证默认态（总开关关）零视觉变化。 */
fun AppState.cardPressShadowEnabled(section: CardSection): Boolean = cardCornerEnabled(section)

// ————————————————————————————————————————————————————————————
// 共用四角读写（组级「板块同步」开启时，圆角组的「四角同步」行用）
// ————————————————————————————————————————————————————————————

/**
 * 共用四角的「四角同步」当前值。
 *
 * 「板块同步」开启后 3 个板块共用同一份设置，以 [CardSection.Item] 的字段为**基准值**读取；
 * 写入时一次改 3 个板块（见 [withSharedCornerSync4] / [withSharedCorner]）。
 */
fun AppState.sharedCornerSync4(): Boolean = cardCornerItemSync4

/** 写共用「四角同步」：同时写 3 个板块的 `Sync4`。 */
fun AppState.withSharedCornerSync4(v: Boolean): AppState =
    copy(
        cardCornerItemSync4 = v,
        cardCornerRecipeSync4 = v,
        cardCornerNpcSync4 = v,
    )

/** 共用模式下某个角的当前值（以 [CardSection.Item] 为基准）。 */
fun AppState.sharedCorner(c: CardCorner): Boolean = cardCornerChildOf(CardSection.Item, c)

/** 写共用模式下某个角：同时写 3 个板块该角。 */
fun AppState.withSharedCorner(c: CardCorner, v: Boolean): AppState =
    cardCornerChildOfSet(CardSection.Item, c, v)
        .cardCornerChildOfSet(CardSection.Recipe, c, v)
        .cardCornerChildOfSet(CardSection.Npc, c, v)

// ————————————————————————————————————————————————————————————
// 内部：把「板块 → 各组的子字段」的 when 收在一处
// ————————————————————————————————————————————————————————————

private fun AppState.cardBgChild(s: CardSection): Boolean = when (s) {
    CardSection.Item -> cardBgItem
    CardSection.Recipe -> cardBgRecipe
    CardSection.Npc -> cardBgNpc
}

private fun AppState.cardCapsuleChild(s: CardSection): Boolean = when (s) {
    CardSection.Item -> cardCapsuleItem
    CardSection.Recipe -> cardCapsuleRecipe
    CardSection.Npc -> cardCapsuleNpc
}

private fun AppState.cardCornerChild(s: CardSection): Boolean = when (s) {
    CardSection.Item -> cardCornerItem
    CardSection.Recipe -> cardCornerRecipe
    CardSection.Npc -> cardCornerNpc
}

private fun AppState.cardCornerSync4(s: CardSection): Boolean = when (s) {
    CardSection.Item -> cardCornerItemSync4
    CardSection.Recipe -> cardCornerRecipeSync4
    CardSection.Npc -> cardCornerNpcSync4
}

private fun AppState.cardCornerChildOf(s: CardSection, c: CardCorner): Boolean = when (s) {
    CardSection.Item -> when (c) {
        CardCorner.TopStart -> cardCornerItemTL
        CardCorner.TopEnd -> cardCornerItemTR
        CardCorner.BottomStart -> cardCornerItemBL
        CardCorner.BottomEnd -> cardCornerItemBR
    }

    CardSection.Recipe -> when (c) {
        CardCorner.TopStart -> cardCornerRecipeTL
        CardCorner.TopEnd -> cardCornerRecipeTR
        CardCorner.BottomStart -> cardCornerRecipeBL
        CardCorner.BottomEnd -> cardCornerRecipeBR
    }

    CardSection.Npc -> when (c) {
        CardCorner.TopStart -> cardCornerNpcTL
        CardCorner.TopEnd -> cardCornerNpcTR
        CardCorner.BottomStart -> cardCornerNpcBL
        CardCorner.BottomEnd -> cardCornerNpcBR
    }
}

// ————————————————————————————————————————————————————————————
// 设置页用的分组描述（让三个组共用同一段 UI）
// ————————————————————————————————————————————————————————————

/**
 * 一组「总开关 + n 板块 + 同步」的读写描述。
 *
 * 设置页拿 [CARD_TOGGLE_GROUPS] 循环渲染即可，不必为三组各写一遍 UI。
 * 所有 setter 都返回新的 [AppState]（不就地修改），调用方交给 `updateAppState` 落盘。
 */
class CardToggleGroup(
    /** 组标题，如「卡片背景」。 */
    val title: String,
    /** 组副标题（说明这组开关控制什么）。 */
    val summary: String,
    /** 总开关当前值。 */
    val masterOf: (AppState) -> Boolean,
    /** 写总开关。 */
    val withMaster: (AppState, Boolean) -> AppState,
    /** 同步开关当前值。 */
    val syncOf: (AppState) -> Boolean,
    /** 写同步开关。 */
    val withSync: (AppState, Boolean) -> AppState,
    /** 某板块的子开关当前值。 */
    val childOf: (AppState, CardSection) -> Boolean,
    /** 写某板块的子开关。 */
    val withChild: (AppState, CardSection, Boolean) -> AppState,
    /** 该组的「板块级」子开关文案后缀，如「背景」→「物品大全 背景」。 */
    val childSuffix: String,
    /** 是否带「四角」二级展开（只有圆角组为 true）。 */
    val withCorners: Boolean = false,
    /** 某板块的四角同步当前值。 */
    val cornerSyncOf: (AppState, CardSection) -> Boolean = { _, _ -> true },
    /** 写某板块的四角同步。 */
    val withCornerSync: (AppState, CardSection, Boolean) -> AppState = { s, _, _ -> s },
    /** 某板块某角当前值。 */
    val cornerOf: (AppState, CardSection, CardCorner) -> Boolean = { _, _, _ -> false },
    /** 写某板块某角。 */
    val withCorner: (AppState, CardSection, CardCorner, Boolean) -> AppState = { s, _, _, _ -> s },
    /** 共用「四角同步」当前值（「板块同步」开启时用；默认恒 true，非圆角组不渲染该行）。 */
    val sharedCornerSyncOf: (AppState) -> Boolean = { true },
    /** 写共用「四角同步」（一次写 3 个板块的 Sync4）。 */
    val withSharedCornerSync: (AppState, Boolean) -> AppState = { s, _ -> s },
    /** 共用某角当前值（以 `CardSection.Item` 为基准）。 */
    val sharedCornerOf: (AppState, CardCorner) -> Boolean = { _, _ -> false },
    /** 写共用某角（一次写 3 个板块该角）。 */
    val withSharedCorner: (AppState, CardCorner, Boolean) -> AppState = { s, _, _ -> s },
) {
    /**
     * 总开关打开后，某板块子开关「实际显示」的值。
     *
     * 同步开时子开关不可单独改，界面按**跟随后的值**显示（而不是存的那份），
     * 否则会出现「总开关开、同步开，子开关却显示关闭」的错位。
     */
    fun effectiveChild(state: AppState, section: CardSection): Boolean =
        if (syncOf(state)) masterOf(state) else childOf(state, section)

    /** 同理：四角同步开时，四角显示跟随板块子开关的值。 */
    fun effectiveCorner(state: AppState, section: CardSection, corner: CardCorner): Boolean =
        if (cornerSyncOf(state, section)) effectiveChild(state, section)
        else cornerOf(state, section, corner)
}

/** 三组开关的描述表。设置页按顺序渲染。 */
val CARD_TOGGLE_GROUPS: List<CardToggleGroup> = listOf(
    CardToggleGroup(
        title = "卡片背景",
        summary = "卡片素材后面的底色方块",
        masterOf = { it.cardBgMaster },
        withMaster = { s, v -> s.copy(cardBgMaster = v) },
        syncOf = { it.cardBgSync },
        withSync = { s, v ->
            if (v) {
                s.copy(cardBgSync = true)
            } else {
                // 关闭板块同步：把总开关当前值落到 3 个板块，避免关闭瞬间三张卡片同时丢背景
                val mv = s.cardBgMaster
                s.copy(cardBgSync = false, cardBgItem = mv, cardBgRecipe = mv, cardBgNpc = mv)
            }
        },
        childOf = { s, sec -> s.cardBgChild(sec) },
        withChild = { s, sec, v ->
            when (sec) {
                CardSection.Item -> s.copy(cardBgItem = v)
                CardSection.Recipe -> s.copy(cardBgRecipe = v)
                CardSection.Npc -> s.copy(cardBgNpc = v)
            }
        },
        childSuffix = "背景",
    ),
    CardToggleGroup(
        title = "卡片圆角",
        summary = "素材背景的四个角；关 = 直角",
        masterOf = { it.cardCornerMaster },
        withMaster = { s, v -> s.copy(cardCornerMaster = v) },
        syncOf = { it.cardCornerSync },
        withSync = { s, v ->
            // 不变式：「板块同步」为开 ⇒ 3 个板块严格共用 Item 基线，三块渲染必须一致。
            // 渲染是**逐板块**求值（cardCornerRadiusOf → cardCornerSync4(section) / cardCornerChildOf(section, c)），
            // 所以凡把同步打开的写操作都必须把 Sync4 与四角值一起归一化，否则界面（读 Item 基线）与实际渲染会分叉。
            if (v) {
                // 打开板块同步：Sync4 与四角全部对齐到 Item 基线（Item 本身不变）。与下面 v=false 分支对称、幂等。
                val shared4 = s.cardCornerItemSync4
                CardCorner.entries.fold(
                    s.copy(
                        cardCornerSync = true,
                        cardCornerRecipeSync4 = shared4,
                        cardCornerNpcSync4 = shared4,
                    ),
                ) { acc, c -> acc.withSharedCorner(c, s.sharedCorner(c)) }
            } else {
                // 关闭板块同步：把总开关值落到 3 个板块，并把「四角同步」统一为当前共用值
                val mv = s.cardCornerMaster
                val shared4 = s.cardCornerItemSync4
                s.copy(
                    cardCornerSync = false,
                    cardCornerItem = mv,
                    cardCornerRecipe = mv,
                    cardCornerNpc = mv,
                    cardCornerItemSync4 = shared4,
                    cardCornerRecipeSync4 = shared4,
                    cardCornerNpcSync4 = shared4,
                )
            }
        },
        childOf = { s, sec -> s.cardCornerChild(sec) },
        withChild = { s, sec, v ->
            when (sec) {
                CardSection.Item -> s.copy(cardCornerItem = v)
                CardSection.Recipe -> s.copy(cardCornerRecipe = v)
                CardSection.Npc -> s.copy(cardCornerNpc = v)
            }
        },
        childSuffix = "圆角",
        withCorners = true,
        cornerSyncOf = { s, sec -> s.cardCornerSync4(sec) },
        withCornerSync = { s, sec, v ->
            if (v) {
                when (sec) {
                    CardSection.Item -> s.copy(cardCornerItemSync4 = true)
                    CardSection.Recipe -> s.copy(cardCornerRecipeSync4 = true)
                    CardSection.Npc -> s.copy(cardCornerNpcSync4 = true)
                }
            } else {
                // 关闭该板块四角同步：把当前生效值（跟随态下 = 板块圆角值）落到该板块的 4 个角
                val eff = if (s.cardCornerSync) s.cardCornerMaster else s.cardCornerChild(sec)
                val off = when (sec) {
                    CardSection.Item -> s.copy(cardCornerItemSync4 = false)
                    CardSection.Recipe -> s.copy(cardCornerRecipeSync4 = false)
                    CardSection.Npc -> s.copy(cardCornerNpcSync4 = false)
                }
                CardCorner.entries.fold(off) { acc, c -> acc.cardCornerChildOfSet(sec, c, eff) }
            }
        },
        cornerOf = { s, sec, c -> s.cardCornerChildOf(sec, c) },
        withCorner = { s, sec, c, v -> s.cardCornerChildOfSet(sec, c, v) },
        sharedCornerSyncOf = { it.sharedCornerSync4() },
        withSharedCornerSync = { s, v ->
            if (v) {
                s.withSharedCornerSync4(true)
            } else {
                // 关闭共用四角同步：把当前生效值（共用/跟随态下四角恒跟总开关）落到 3 板块 × 4 角
                val eff = s.cardCornerMaster
                CardCorner.entries.fold(s.withSharedCornerSync4(false)) { acc, c ->
                    acc.withSharedCorner(c, eff)
                }
            }
        },
        sharedCornerOf = { s, c -> s.sharedCorner(c) },
        withSharedCorner = { s, c, v -> s.withSharedCorner(c, v) },
    ),
    CardToggleGroup(
        title = "胶囊背景",
        summary = "名称那行的蓝底胶囊；关 = 纯文字",
        masterOf = { it.cardCapsuleMaster },
        withMaster = { s, v -> s.copy(cardCapsuleMaster = v) },
        syncOf = { it.cardCapsuleSync },
        withSync = { s, v ->
            if (v) {
                s.copy(cardCapsuleSync = true)
            } else {
                // 关闭板块同步：把总开关当前值落到 3 个板块，避免关闭瞬间三处胶囊同时消失
                val mv = s.cardCapsuleMaster
                s.copy(cardCapsuleSync = false, cardCapsuleItem = mv, cardCapsuleRecipe = mv, cardCapsuleNpc = mv)
            }
        },
        childOf = { s, sec -> s.cardCapsuleChild(sec) },
        withChild = { s, sec, v ->
            when (sec) {
                CardSection.Item -> s.copy(cardCapsuleItem = v)
                CardSection.Recipe -> s.copy(cardCapsuleRecipe = v)
                CardSection.Npc -> s.copy(cardCapsuleNpc = v)
            }
        },
        childSuffix = "胶囊",
    ),
)

private fun AppState.cardCornerChildOfSet(s: CardSection, c: CardCorner, v: Boolean): AppState =
    when (s) {
        CardSection.Item -> when (c) {
            CardCorner.TopStart -> copy(cardCornerItemTL = v)
            CardCorner.TopEnd -> copy(cardCornerItemTR = v)
            CardCorner.BottomStart -> copy(cardCornerItemBL = v)
            CardCorner.BottomEnd -> copy(cardCornerItemBR = v)
        }

        CardSection.Recipe -> when (c) {
            CardCorner.TopStart -> copy(cardCornerRecipeTL = v)
            CardCorner.TopEnd -> copy(cardCornerRecipeTR = v)
            CardCorner.BottomStart -> copy(cardCornerRecipeBL = v)
            CardCorner.BottomEnd -> copy(cardCornerRecipeBR = v)
        }

        CardSection.Npc -> when (c) {
            CardCorner.TopStart -> copy(cardCornerNpcTL = v)
            CardCorner.TopEnd -> copy(cardCornerNpcTR = v)
            CardCorner.BottomStart -> copy(cardCornerNpcBL = v)
            CardCorner.BottomEnd -> copy(cardCornerNpcBR = v)
        }
    }
