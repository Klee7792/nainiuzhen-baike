package com.nainiuzhen.wiki.platform

import android.content.Context
import android.content.SharedPreferences
import com.nainiuzhen.wiki.utils.AppSettingsStore
import com.nainiuzhen.wiki.utils.AppState

/**
 * 基于 `SharedPreferences` 的 [AppSettingsStore] 实现（Android 端）。
 *
 * 文件：`getSharedPreferences("app_settings", MODE_PRIVATE)`。
 * 持久化全部标量偏好（主题 colorMode/monet、圆角/模糊/转场/滑动返回，以及 v5 新增的 14 个开关），
 * 保证重启后设置不丢失。
 */
class AndroidAppSettingsStore(context: Context) : AppSettingsStore {
    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    override fun load(): AppState = AppState(
        colorMode = prefs.getInt(KEY_COLOR_MODE, 0),
        monet = prefs.getBoolean(KEY_MONET, false),
        enableBlur = prefs.getBoolean(KEY_BLUR, true),
        enableSquircle = prefs.getBoolean(KEY_SQUIRCLE, true),
        navTransitionStyle = prefs.getInt(KEY_TRANSITION, 0),
        enableSwipeBack = prefs.getBoolean(KEY_SWIPE_BACK, true),
        enableCornerClip = prefs.getBoolean(KEY_CORNER_CLIP, true),
        scrollEndHaptic = prefs.getBoolean(KEY_SCROLL_HAPTIC, true),
        pageUserScroll = prefs.getBoolean(KEY_PAGE_SCROLL, true),
        showTopAppBar = prefs.getBoolean(KEY_SHOW_TOP_BAR, true),
        topAppBarBlurStyle = prefs.getInt(KEY_TOP_BAR_BLUR_STYLE, 0),
        showNavigationBar = prefs.getBoolean(KEY_SHOW_NAV_BAR, true),
        showNavigationBadge = prefs.getBoolean(KEY_SHOW_NAV_BADGE, false),
        navigationBarMode = prefs.getInt(KEY_NAV_BAR_MODE, 0),
        useFloatingNavigationBar = prefs.getBoolean(KEY_FLOATING_NAV_BAR, true),
        showFloatingToolbar = prefs.getBoolean(KEY_FLOATING_TOOLBAR, false),
        floatingToolbarPosition = prefs.getInt(KEY_FLOATING_TOOLBAR_POS, 0),
        showFloatingActionButton = prefs.getBoolean(KEY_FLOATING_FAB, false),
        floatingActionButtonPosition = prefs.getInt(KEY_FLOATING_FAB_POS, 0),
        enableDim = prefs.getBoolean(KEY_DIM, false),
        blockInputDuringTransition = prefs.getBoolean(KEY_BLOCK_INPUT, true),
        floatingNavigationBarStyle = prefs.getInt(KEY_FLOATING_NAV_BAR_STYLE, 1),
        floatingNavigationBarPosition = prefs.getInt(KEY_FLOATING_NAV_BAR_POS, 0),
        homeImageScale = prefs.getScaleFloat(KEY_HOME_IMAGE_SCALE, 8f),
        cardImageScale = prefs.getScaleFloat(KEY_CARD_IMAGE_SCALE, 5f),
        dialogBodyImageScale = prefs.getScaleFloat(KEY_DIALOG_BODY_IMAGE_SCALE, 6f),
        dialogRecipeImageScale = prefs.getScaleFloat(KEY_DIALOG_RECIPE_IMAGE_SCALE, 6f),
        dialogFavHateImageScale = prefs.getScaleFloat(KEY_DIALOG_FAV_HATE_IMAGE_SCALE, 6f),
        scaleStep = prefs.getScaleFloat(KEY_SCALE_STEP, 0.1f),
        itemCardTextSizeSp = prefs.getScaleFloat(KEY_ITEM_CARD_TEXT_SIZE_SP, 10f),
        recipeCardTextSizeSp = prefs.getScaleFloat(KEY_RECIPE_CARD_TEXT_SIZE_SP, 10f),
        allowPhoneLandscape = prefs.getBoolean(KEY_ALLOW_PHONE_LANDSCAPE, true),
        monetSeed = prefs.getInt(KEY_MONET_SEED, 0),
        // —— v31 卡片外观设置（三组「总开关 + n 板块 + 同步」+ 四角 + 按下阴影同步）—— //
        cardBgMaster = prefs.getBoolean(KEY_CARD_BG_MASTER, false),
        cardBgSync = prefs.getBoolean(KEY_CARD_BG_SYNC, true),
        cardBgItem = prefs.getBoolean(KEY_CARD_BG_ITEM, false),
        cardBgRecipe = prefs.getBoolean(KEY_CARD_BG_RECIPE, false),
        cardBgNpc = prefs.getBoolean(KEY_CARD_BG_NPC, false),
        cardCornerMaster = prefs.getBoolean(KEY_CARD_CORNER_MASTER, false),
        cardCornerSync = prefs.getBoolean(KEY_CARD_CORNER_SYNC, true),
        cardCornerItem = prefs.getBoolean(KEY_CARD_CORNER_ITEM, false),
        cardCornerRecipe = prefs.getBoolean(KEY_CARD_CORNER_RECIPE, false),
        cardCornerNpc = prefs.getBoolean(KEY_CARD_CORNER_NPC, false),
        cardCornerItemTL = prefs.getBoolean(KEY_CARD_CORNER_ITEM_TL, false),
        cardCornerItemTR = prefs.getBoolean(KEY_CARD_CORNER_ITEM_TR, false),
        cardCornerItemBL = prefs.getBoolean(KEY_CARD_CORNER_ITEM_BL, false),
        cardCornerItemBR = prefs.getBoolean(KEY_CARD_CORNER_ITEM_BR, false),
        cardCornerItemSync4 = prefs.getBoolean(KEY_CARD_CORNER_ITEM_SYNC4, true),
        cardCornerRecipeTL = prefs.getBoolean(KEY_CARD_CORNER_RECIPE_TL, false),
        cardCornerRecipeTR = prefs.getBoolean(KEY_CARD_CORNER_RECIPE_TR, false),
        cardCornerRecipeBL = prefs.getBoolean(KEY_CARD_CORNER_RECIPE_BL, false),
        cardCornerRecipeBR = prefs.getBoolean(KEY_CARD_CORNER_RECIPE_BR, false),
        cardCornerRecipeSync4 = prefs.getBoolean(KEY_CARD_CORNER_RECIPE_SYNC4, true),
        cardCornerNpcTL = prefs.getBoolean(KEY_CARD_CORNER_NPC_TL, false),
        cardCornerNpcTR = prefs.getBoolean(KEY_CARD_CORNER_NPC_TR, false),
        cardCornerNpcBL = prefs.getBoolean(KEY_CARD_CORNER_NPC_BL, false),
        cardCornerNpcBR = prefs.getBoolean(KEY_CARD_CORNER_NPC_BR, false),
        cardCornerNpcSync4 = prefs.getBoolean(KEY_CARD_CORNER_NPC_SYNC4, true),
        cardPressShadowSync = prefs.getBoolean(KEY_CARD_PRESS_SHADOW_SYNC, true),
        cardCapsuleMaster = prefs.getBoolean(KEY_CARD_CAPSULE_MASTER, false),
        cardCapsuleSync = prefs.getBoolean(KEY_CARD_CAPSULE_SYNC, true),
        cardCapsuleItem = prefs.getBoolean(KEY_CARD_CAPSULE_ITEM, false),
        cardCapsuleRecipe = prefs.getBoolean(KEY_CARD_CAPSULE_RECIPE, false),
        cardCapsuleNpc = prefs.getBoolean(KEY_CARD_CAPSULE_NPC, false),
    )

    override fun save(state: AppState) {
        prefs.edit().apply {
            putInt(KEY_COLOR_MODE, state.colorMode)
            putBoolean(KEY_MONET, state.monet)
            putBoolean(KEY_BLUR, state.enableBlur)
            putBoolean(KEY_SQUIRCLE, state.enableSquircle)
            putInt(KEY_TRANSITION, state.navTransitionStyle)
            putBoolean(KEY_SWIPE_BACK, state.enableSwipeBack)
            putBoolean(KEY_CORNER_CLIP, state.enableCornerClip)
            putBoolean(KEY_SCROLL_HAPTIC, state.scrollEndHaptic)
            putBoolean(KEY_PAGE_SCROLL, state.pageUserScroll)
            putBoolean(KEY_SHOW_TOP_BAR, state.showTopAppBar)
            putInt(KEY_TOP_BAR_BLUR_STYLE, state.topAppBarBlurStyle)
            putBoolean(KEY_SHOW_NAV_BAR, state.showNavigationBar)
            putBoolean(KEY_SHOW_NAV_BADGE, state.showNavigationBadge)
            putInt(KEY_NAV_BAR_MODE, state.navigationBarMode)
            putBoolean(KEY_FLOATING_NAV_BAR, state.useFloatingNavigationBar)
            putBoolean(KEY_FLOATING_TOOLBAR, state.showFloatingToolbar)
            putInt(KEY_FLOATING_TOOLBAR_POS, state.floatingToolbarPosition)
            putBoolean(KEY_FLOATING_FAB, state.showFloatingActionButton)
            putInt(KEY_FLOATING_FAB_POS, state.floatingActionButtonPosition)
            putBoolean(KEY_DIM, state.enableDim)
            putBoolean(KEY_BLOCK_INPUT, state.blockInputDuringTransition)
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
            putBoolean(KEY_ALLOW_PHONE_LANDSCAPE, state.allowPhoneLandscape)
            putInt(KEY_MONET_SEED, state.monetSeed)
            // —— v31 卡片外观设置 —— //
            putBoolean(KEY_CARD_BG_MASTER, state.cardBgMaster)
            putBoolean(KEY_CARD_BG_SYNC, state.cardBgSync)
            putBoolean(KEY_CARD_BG_ITEM, state.cardBgItem)
            putBoolean(KEY_CARD_BG_RECIPE, state.cardBgRecipe)
            putBoolean(KEY_CARD_BG_NPC, state.cardBgNpc)
            putBoolean(KEY_CARD_CORNER_MASTER, state.cardCornerMaster)
            putBoolean(KEY_CARD_CORNER_SYNC, state.cardCornerSync)
            putBoolean(KEY_CARD_CORNER_ITEM, state.cardCornerItem)
            putBoolean(KEY_CARD_CORNER_RECIPE, state.cardCornerRecipe)
            putBoolean(KEY_CARD_CORNER_NPC, state.cardCornerNpc)
            putBoolean(KEY_CARD_CORNER_ITEM_TL, state.cardCornerItemTL)
            putBoolean(KEY_CARD_CORNER_ITEM_TR, state.cardCornerItemTR)
            putBoolean(KEY_CARD_CORNER_ITEM_BL, state.cardCornerItemBL)
            putBoolean(KEY_CARD_CORNER_ITEM_BR, state.cardCornerItemBR)
            putBoolean(KEY_CARD_CORNER_ITEM_SYNC4, state.cardCornerItemSync4)
            putBoolean(KEY_CARD_CORNER_RECIPE_TL, state.cardCornerRecipeTL)
            putBoolean(KEY_CARD_CORNER_RECIPE_TR, state.cardCornerRecipeTR)
            putBoolean(KEY_CARD_CORNER_RECIPE_BL, state.cardCornerRecipeBL)
            putBoolean(KEY_CARD_CORNER_RECIPE_BR, state.cardCornerRecipeBR)
            putBoolean(KEY_CARD_CORNER_RECIPE_SYNC4, state.cardCornerRecipeSync4)
            putBoolean(KEY_CARD_CORNER_NPC_TL, state.cardCornerNpcTL)
            putBoolean(KEY_CARD_CORNER_NPC_TR, state.cardCornerNpcTR)
            putBoolean(KEY_CARD_CORNER_NPC_BL, state.cardCornerNpcBL)
            putBoolean(KEY_CARD_CORNER_NPC_BR, state.cardCornerNpcBR)
            putBoolean(KEY_CARD_CORNER_NPC_SYNC4, state.cardCornerNpcSync4)
            putBoolean(KEY_CARD_PRESS_SHADOW_SYNC, state.cardPressShadowSync)
            putBoolean(KEY_CARD_CAPSULE_MASTER, state.cardCapsuleMaster)
            putBoolean(KEY_CARD_CAPSULE_SYNC, state.cardCapsuleSync)
            putBoolean(KEY_CARD_CAPSULE_ITEM, state.cardCapsuleItem)
            putBoolean(KEY_CARD_CAPSULE_RECIPE, state.cardCapsuleRecipe)
            putBoolean(KEY_CARD_CAPSULE_NPC, state.cardCapsuleNpc)
            apply()
        }
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

/**
 * 兼容迁移读取：旧版本把倍率以 Int 固化进 SharedPreferences，升级到 Float 后直接
 * [android.content.SharedPreferences.getFloat] 会抛 `Integer cannot be cast to Float`。
 * 这里按实际存储类型读取：Float 直接用、Int 转 Float、缺失则回退默认值。
 */
private fun SharedPreferences.getScaleFloat(key: String, default: Float): Float {
    return when (val v = this.all[key]) {
        is Float -> v
        is Int -> v.toFloat()
        else -> default
    }
}
