package com.nainiuzhen.wiki.platform

import com.nainiuzhen.wiki.utils.AppSettingsStore
import com.nainiuzhen.wiki.utils.AppState
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSNumber

/**
 * 基于 `NSUserDefaults` 的 [AppSettingsStore] 实现（iOS 端）。
 *
 * 键名与 Android `SharedPreferences` 版本完全一致（`app_settings` 语义由 suite 名承载，
 * 这里直接用 standardUserDefaults）。数值统一走 NSNumber：Int / Float 在 K/N 桥接里
 * 经 objectForKey 读回 [NSNumber] 后按需转换，兼容「旧版本 Int 固化、新版 Float 读取」
 * 的历史迁移场景（同 Android 端 getScaleFloat 的容错语义）。
 */
class IosAppSettingsStore : AppSettingsStore {

    private val defaults = NSUserDefaults.standardUserDefaults

    // ---- 读取辅助（缺失一律回退默认值，与 Android 行为一致）----

    private fun intOf(key: String, default: Int): Int =
        (defaults.objectForKey(key) as? NSNumber)?.integerValue?.toInt() ?: default

    private fun floatOf(key: String, default: Float): Float =
        (defaults.objectForKey(key) as? NSNumber)?.floatValue ?: default

    private fun boolOf(key: String, default: Boolean): Boolean =
        (defaults.objectForKey(key) as? NSNumber)?.boolValue ?: default

    // ---- 写入辅助（数值统一 setDouble，读取侧 NSNumber 自适配类型）----

    private fun putInt(key: String, v: Int) = defaults.setDouble(v.toDouble(), forKey = key)
    private fun putFloat(key: String, v: Float) = defaults.setDouble(v.toDouble(), forKey = key)
    private fun putBool(key: String, v: Boolean) = defaults.setBool(v, forKey = key)

    override fun load(): AppState = AppState(
        colorMode = intOf(KEY_COLOR_MODE, 0),
        monet = boolOf(KEY_MONET, false),
        enableBlur = boolOf(KEY_BLUR, true),
        enableSquircle = boolOf(KEY_SQUIRCLE, true),
        navTransitionStyle = intOf(KEY_TRANSITION, 0),
        enableSwipeBack = boolOf(KEY_SWIPE_BACK, true),
        enableCornerClip = boolOf(KEY_CORNER_CLIP, true),
        scrollEndHaptic = boolOf(KEY_SCROLL_HAPTIC, true),
        pageUserScroll = boolOf(KEY_PAGE_SCROLL, true),
        showTopAppBar = boolOf(KEY_SHOW_TOP_BAR, true),
        topAppBarBlurStyle = intOf(KEY_TOP_BAR_BLUR_STYLE, 0),
        showNavigationBar = boolOf(KEY_SHOW_NAV_BAR, true),
        showNavigationBadge = boolOf(KEY_SHOW_NAV_BADGE, false),
        navigationBarMode = intOf(KEY_NAV_BAR_MODE, 0),
        useFloatingNavigationBar = boolOf(KEY_FLOATING_NAV_BAR, true),
        showFloatingToolbar = boolOf(KEY_FLOATING_TOOLBAR, false),
        floatingToolbarPosition = intOf(KEY_FLOATING_TOOLBAR_POS, 0),
        showFloatingActionButton = boolOf(KEY_FLOATING_FAB, false),
        floatingActionButtonPosition = intOf(KEY_FLOATING_FAB_POS, 0),
        enableDim = boolOf(KEY_DIM, false),
        blockInputDuringTransition = boolOf(KEY_BLOCK_INPUT, true),
        floatingNavigationBarStyle = intOf(KEY_FLOATING_NAV_BAR_STYLE, 1),
        floatingNavigationBarPosition = intOf(KEY_FLOATING_NAV_BAR_POS, 0),
        homeImageScale = floatOf(KEY_HOME_IMAGE_SCALE, 8f),
        cardImageScale = floatOf(KEY_CARD_IMAGE_SCALE, 5f),
        dialogBodyImageScale = floatOf(KEY_DIALOG_BODY_IMAGE_SCALE, 6f),
        dialogRecipeImageScale = floatOf(KEY_DIALOG_RECIPE_IMAGE_SCALE, 6f),
        dialogFavHateImageScale = floatOf(KEY_DIALOG_FAV_HATE_IMAGE_SCALE, 6f),
        scaleStep = floatOf(KEY_SCALE_STEP, 0.1f),
        itemCardTextSizeSp = floatOf(KEY_ITEM_CARD_TEXT_SIZE_SP, 10f),
        recipeCardTextSizeSp = floatOf(KEY_RECIPE_CARD_TEXT_SIZE_SP, 10f),
        allowPhoneLandscape = boolOf(KEY_ALLOW_PHONE_LANDSCAPE, true),
        monetSeed = intOf(KEY_MONET_SEED, 0),
        scheduleFilterPinned = boolOf(KEY_SCHEDULE_FILTER_PINNED, true),
        // —— v31 卡片外观设置 —— //
        cardBgMaster = boolOf(KEY_CARD_BG_MASTER, false),
        cardBgSync = boolOf(KEY_CARD_BG_SYNC, true),
        cardBgItem = boolOf(KEY_CARD_BG_ITEM, false),
        cardBgRecipe = boolOf(KEY_CARD_BG_RECIPE, false),
        cardBgNpc = boolOf(KEY_CARD_BG_NPC, false),
        cardCornerMaster = boolOf(KEY_CARD_CORNER_MASTER, false),
        cardCornerSync = boolOf(KEY_CARD_CORNER_SYNC, true),
        cardCornerItem = boolOf(KEY_CARD_CORNER_ITEM, false),
        cardCornerRecipe = boolOf(KEY_CARD_CORNER_RECIPE, false),
        cardCornerNpc = boolOf(KEY_CARD_CORNER_NPC, false),
        cardCornerItemTL = boolOf(KEY_CARD_CORNER_ITEM_TL, false),
        cardCornerItemTR = boolOf(KEY_CARD_CORNER_ITEM_TR, false),
        cardCornerItemBL = boolOf(KEY_CARD_CORNER_ITEM_BL, false),
        cardCornerItemBR = boolOf(KEY_CARD_CORNER_ITEM_BR, false),
        cardCornerItemSync4 = boolOf(KEY_CARD_CORNER_ITEM_SYNC4, true),
        cardCornerRecipeTL = boolOf(KEY_CARD_CORNER_RECIPE_TL, false),
        cardCornerRecipeTR = boolOf(KEY_CARD_CORNER_RECIPE_TR, false),
        cardCornerRecipeBL = boolOf(KEY_CARD_CORNER_RECIPE_BL, false),
        cardCornerRecipeBR = boolOf(KEY_CARD_CORNER_RECIPE_BR, false),
        cardCornerRecipeSync4 = boolOf(KEY_CARD_CORNER_RECIPE_SYNC4, true),
        cardCornerNpcTL = boolOf(KEY_CARD_CORNER_NPC_TL, false),
        cardCornerNpcTR = boolOf(KEY_CARD_CORNER_NPC_TR, false),
        cardCornerNpcBL = boolOf(KEY_CARD_CORNER_NPC_BL, false),
        cardCornerNpcBR = boolOf(KEY_CARD_CORNER_NPC_BR, false),
        cardCornerNpcSync4 = boolOf(KEY_CARD_CORNER_NPC_SYNC4, true),
        cardPressShadowSync = boolOf(KEY_CARD_PRESS_SHADOW_SYNC, true),
        cardCapsuleMaster = boolOf(KEY_CARD_CAPSULE_MASTER, false),
        cardCapsuleSync = boolOf(KEY_CARD_CAPSULE_SYNC, true),
        cardCapsuleItem = boolOf(KEY_CARD_CAPSULE_ITEM, false),
        cardCapsuleRecipe = boolOf(KEY_CARD_CAPSULE_RECIPE, false),
        cardCapsuleNpc = boolOf(KEY_CARD_CAPSULE_NPC, false),
    )

    override fun save(state: AppState) {
        putInt(KEY_COLOR_MODE, state.colorMode)
        putBool(KEY_MONET, state.monet)
        putBool(KEY_BLUR, state.enableBlur)
        putBool(KEY_SQUIRCLE, state.enableSquircle)
        putInt(KEY_TRANSITION, state.navTransitionStyle)
        putBool(KEY_SWIPE_BACK, state.enableSwipeBack)
        putBool(KEY_CORNER_CLIP, state.enableCornerClip)
        putBool(KEY_SCROLL_HAPTIC, state.scrollEndHaptic)
        putBool(KEY_PAGE_SCROLL, state.pageUserScroll)
        putBool(KEY_SHOW_TOP_BAR, state.showTopAppBar)
        putInt(KEY_TOP_BAR_BLUR_STYLE, state.topAppBarBlurStyle)
        putBool(KEY_SHOW_NAV_BAR, state.showNavigationBar)
        putBool(KEY_SHOW_NAV_BADGE, state.showNavigationBadge)
        putInt(KEY_NAV_BAR_MODE, state.navigationBarMode)
        putBool(KEY_FLOATING_NAV_BAR, state.useFloatingNavigationBar)
        putBool(KEY_FLOATING_TOOLBAR, state.showFloatingToolbar)
        putInt(KEY_FLOATING_TOOLBAR_POS, state.floatingToolbarPosition)
        putBool(KEY_FLOATING_FAB, state.showFloatingActionButton)
        putInt(KEY_FLOATING_FAB_POS, state.floatingActionButtonPosition)
        putBool(KEY_DIM, state.enableDim)
        putBool(KEY_BLOCK_INPUT, state.blockInputDuringTransition)
        putInt(KEY_FLOATING_NAV_BAR_STYLE, state.floatingNavigationBarStyle)
        putInt(KEY_FLOATING_NAV_BAR_POS, state.floatingNavigationBarPosition)
        putFloat(KEY_HOME_IMAGE_SCALE, state.homeImageScale)
        putFloat(KEY_CARD_IMAGE_SCALE, state.cardImageScale)
        putFloat(KEY_DIALOG_BODY_IMAGE_SCALE, state.dialogBodyImageScale)
        putFloat(KEY_DIALOG_RECIPE_IMAGE_SCALE, state.dialogRecipeImageScale)
        putFloat(KEY_DIALOG_FAV_HATE_IMAGE_SCALE, state.dialogFavHateImageScale)
        putFloat(KEY_SCALE_STEP, state.scaleStep)
        putFloat(KEY_ITEM_CARD_TEXT_SIZE_SP, state.itemCardTextSizeSp)
        putFloat(KEY_RECIPE_CARD_TEXT_SIZE_SP, state.recipeCardTextSizeSp)
        putBool(KEY_ALLOW_PHONE_LANDSCAPE, state.allowPhoneLandscape)
        putInt(KEY_MONET_SEED, state.monetSeed)
        putBool(KEY_SCHEDULE_FILTER_PINNED, state.scheduleFilterPinned)
        // —— v31 卡片外观设置 —— //
        putBool(KEY_CARD_BG_MASTER, state.cardBgMaster)
        putBool(KEY_CARD_BG_SYNC, state.cardBgSync)
        putBool(KEY_CARD_BG_ITEM, state.cardBgItem)
        putBool(KEY_CARD_BG_RECIPE, state.cardBgRecipe)
        putBool(KEY_CARD_BG_NPC, state.cardBgNpc)
        putBool(KEY_CARD_CORNER_MASTER, state.cardCornerMaster)
        putBool(KEY_CARD_CORNER_SYNC, state.cardCornerSync)
        putBool(KEY_CARD_CORNER_ITEM, state.cardCornerItem)
        putBool(KEY_CARD_CORNER_RECIPE, state.cardCornerRecipe)
        putBool(KEY_CARD_CORNER_NPC, state.cardCornerNpc)
        putBool(KEY_CARD_CORNER_ITEM_TL, state.cardCornerItemTL)
        putBool(KEY_CARD_CORNER_ITEM_TR, state.cardCornerItemTR)
        putBool(KEY_CARD_CORNER_ITEM_BL, state.cardCornerItemBL)
        putBool(KEY_CARD_CORNER_ITEM_BR, state.cardCornerItemBR)
        putBool(KEY_CARD_CORNER_ITEM_SYNC4, state.cardCornerItemSync4)
        putBool(KEY_CARD_CORNER_RECIPE_TL, state.cardCornerRecipeTL)
        putBool(KEY_CARD_CORNER_RECIPE_TR, state.cardCornerRecipeTR)
        putBool(KEY_CARD_CORNER_RECIPE_BL, state.cardCornerRecipeBL)
        putBool(KEY_CARD_CORNER_RECIPE_BR, state.cardCornerRecipeBR)
        putBool(KEY_CARD_CORNER_RECIPE_SYNC4, state.cardCornerRecipeSync4)
        putBool(KEY_CARD_CORNER_NPC_TL, state.cardCornerNpcTL)
        putBool(KEY_CARD_CORNER_NPC_TR, state.cardCornerNpcTR)
        putBool(KEY_CARD_CORNER_NPC_BL, state.cardCornerNpcBL)
        putBool(KEY_CARD_CORNER_NPC_BR, state.cardCornerNpcBR)
        putBool(KEY_CARD_CORNER_NPC_SYNC4, state.cardCornerNpcSync4)
        putBool(KEY_CARD_PRESS_SHADOW_SYNC, state.cardPressShadowSync)
        putBool(KEY_CARD_CAPSULE_MASTER, state.cardCapsuleMaster)
        putBool(KEY_CARD_CAPSULE_SYNC, state.cardCapsuleSync)
        putBool(KEY_CARD_CAPSULE_ITEM, state.cardCapsuleItem)
        putBool(KEY_CARD_CAPSULE_RECIPE, state.cardCapsuleRecipe)
        putBool(KEY_CARD_CAPSULE_NPC, state.cardCapsuleNpc)
        // NSUserDefaults 自动落盘；显式 synchronize 一次，确保立即写盘（进程被杀也不丢）。
        defaults.synchronize()
    }

    private companion object {
        const val KEY_COLOR_MODE = "colorMode"
        const val KEY_MONET = "monet"
        const val KEY_BLUR = "enableBlur"
        const val KEY_SQUIRCLE = "enableSquircle"
        const val KEY_TRANSITION = "navTransitionStyle"
        const val KEY_SWIPE_BACK = "enableSwipeBack"
        const val KEY_CORNER_CLIP = "enableCornerClip"
        const val KEY_SCROLL_HAPTIC = "scrollEndHaptic"
        const val KEY_PAGE_SCROLL = "pageUserScroll"
        const val KEY_SHOW_TOP_BAR = "showTopAppBar"
        const val KEY_TOP_BAR_BLUR_STYLE = "topAppBarBlurStyle"
        const val KEY_SHOW_NAV_BAR = "showNavigationBar"
        const val KEY_SHOW_NAV_BADGE = "showNavigationBadge"
        const val KEY_NAV_BAR_MODE = "navigationBarMode"
        const val KEY_FLOATING_NAV_BAR = "useFloatingNavigationBar"
        const val KEY_FLOATING_TOOLBAR = "showFloatingToolbar"
        const val KEY_FLOATING_TOOLBAR_POS = "floatingToolbarPosition"
        const val KEY_FLOATING_FAB = "showFloatingActionButton"
        const val KEY_FLOATING_FAB_POS = "floatingActionButtonPosition"
        const val KEY_DIM = "enableDim"
        const val KEY_BLOCK_INPUT = "blockInputDuringTransition"
        const val KEY_FLOATING_NAV_BAR_STYLE = "floatingNavigationBarStyle"
        const val KEY_FLOATING_NAV_BAR_POS = "floatingNavigationBarPosition"
        const val KEY_HOME_IMAGE_SCALE = "homeImageScale"
        const val KEY_CARD_IMAGE_SCALE = "cardImageScale"
        const val KEY_DIALOG_BODY_IMAGE_SCALE = "dialogBodyImageScale"
        const val KEY_DIALOG_RECIPE_IMAGE_SCALE = "dialogRecipeImageScale"
        const val KEY_DIALOG_FAV_HATE_IMAGE_SCALE = "dialogFavHateImageScale"
        const val KEY_SCALE_STEP = "scaleStep"
        const val KEY_ITEM_CARD_TEXT_SIZE_SP = "itemCardTextSizeSp"
        const val KEY_RECIPE_CARD_TEXT_SIZE_SP = "recipeCardTextSizeSp"
        const val KEY_ALLOW_PHONE_LANDSCAPE = "allowPhoneLandscape"
        const val KEY_MONET_SEED = "monetSeed"
        const val KEY_SCHEDULE_FILTER_PINNED = "scheduleFilterPinned"
        // —— v31 卡片外观设置 —— //
        const val KEY_CARD_BG_MASTER = "cardBgMaster"
        const val KEY_CARD_BG_SYNC = "cardBgSync"
        const val KEY_CARD_BG_ITEM = "cardBgItem"
        const val KEY_CARD_BG_RECIPE = "cardBgRecipe"
        const val KEY_CARD_BG_NPC = "cardBgNpc"
        const val KEY_CARD_CORNER_MASTER = "cardCornerMaster"
        const val KEY_CARD_CORNER_SYNC = "cardCornerSync"
        const val KEY_CARD_CORNER_ITEM = "cardCornerItem"
        const val KEY_CARD_CORNER_RECIPE = "cardCornerRecipe"
        const val KEY_CARD_CORNER_NPC = "cardCornerNpc"
        const val KEY_CARD_CORNER_ITEM_TL = "cardCornerItemTL"
        const val KEY_CARD_CORNER_ITEM_TR = "cardCornerItemTR"
        const val KEY_CARD_CORNER_ITEM_BL = "cardCornerItemBL"
        const val KEY_CARD_CORNER_ITEM_BR = "cardCornerItemBR"
        const val KEY_CARD_CORNER_ITEM_SYNC4 = "cardCornerItemSync4"
        const val KEY_CARD_CORNER_RECIPE_TL = "cardCornerRecipeTL"
        const val KEY_CARD_CORNER_RECIPE_TR = "cardCornerRecipeTR"
        const val KEY_CARD_CORNER_RECIPE_BL = "cardCornerRecipeBL"
        const val KEY_CARD_CORNER_RECIPE_BR = "cardCornerRecipeBR"
        const val KEY_CARD_CORNER_RECIPE_SYNC4 = "cardCornerRecipeSync4"
        const val KEY_CARD_CORNER_NPC_TL = "cardCornerNpcTL"
        const val KEY_CARD_CORNER_NPC_TR = "cardCornerNpcTR"
        const val KEY_CARD_CORNER_NPC_BL = "cardCornerNpcBL"
        const val KEY_CARD_CORNER_NPC_BR = "cardCornerNpcBR"
        const val KEY_CARD_CORNER_NPC_SYNC4 = "cardCornerNpcSync4"
        const val KEY_CARD_PRESS_SHADOW_SYNC = "cardPressShadowSync"
        const val KEY_CARD_CAPSULE_MASTER = "cardCapsuleMaster"
        const val KEY_CARD_CAPSULE_SYNC = "cardCapsuleSync"
        const val KEY_CARD_CAPSULE_ITEM = "cardCapsuleItem"
        const val KEY_CARD_CAPSULE_RECIPE = "cardCapsuleRecipe"
        const val KEY_CARD_CAPSULE_NPC = "cardCapsuleNpc"
    }
}
