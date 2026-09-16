package com.nainiuzhen.wiki.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.nainiuzhen.wiki.ui.nav.LocalSpriteRepository
import com.nainiuzhen.wiki.utils.LocalAppSettings
import com.nainiuzhen.wiki.utils.IoDispatcher
import kotlinx.coroutines.withContext

/**
 * 图片放大倍率的上下文：从 [com.nainiuzhen.wiki.utils.LocalAppSettings] 取对应倍率，
 * 实现「设置子页统一调倍率」。传入 [SpriteImage.scaleContext] 即按该上下文的倍率做严格整数倍显示，
 * 比显式 [SpriteImage.pixelScale] 更便于用户在设置页一处调整全站同类图片。
 */
enum class SpriteScaleContext {
    /** 主页板块图（物品 / 配方入口卡片图）。 */
    Home,

    /** 物品 / 配方卡片内图片。 */
    Card,

    /** 详情 dialog 头部素材（物品 / 配方详情页名称区左侧）。 */
    DialogBody,

    /** 弹窗配方素材：配方原料 / 产物。 */
    DialogRecipe,

    /** 弹窗喜恶素材：NPC 最爱 / 喜欢 / 讨厌。 */
    DialogFavHate,
}

/**
 * 切片图显示组件：根据帧名 [frameKey] 从 [LocalSpriteRepository] 取图并渲染。
 *
 * [filterQuality]：像素风素材传 [FilterQuality.None] 即最近邻（硬边、不模糊）放大，得到与
 * 「切片对比图 x5」一致的硬边像素效果；普通素材（默认 [FilterQuality.Medium]）走双线性平滑。
 *
 * [scaleContext]（推荐）：按上下文从 [LocalAppSettings] 取倍率做整数倍显示——
 * [SpriteScaleContext.Home]→`homeImageScale`（默认 8）、
 * [SpriteScaleContext.Card]→`cardImageScale`（默认 5）、
 * [SpriteScaleContext.DialogBody]→`dialogBodyImageScale`（默认 6）、
 * [SpriteScaleContext.DialogRecipe]→`dialogRecipeImageScale`（默认 6）、
 * [SpriteScaleContext.DialogFavHate]→`dialogFavHateImageScale`（默认 6）。
 *
 * [pixelScale]（兜底）：显式倍率（Float），仅在 [scaleContext] 为 null 时生效，
 * 便于非设置项场景（如固定倍率）直接使用。
 *
 * 取图（首启动切片 / 解码 / 写缓存）放到 IO 线程执行，先用占位图显示、加载完成后再替换，
 * 避免在主线程同步切片导致首屏或进入物品页卡顿（启动变慢排查 #1：同步切片阻塞 UI 线程，
 * 尤其版本号变更触发全量重切时最为明显）。取图结果只依赖帧名，与倍率无关，故 produceState
 * 的 key 不含倍率，避免拖动倍率滑块时重复切片。
 */
@Composable
fun SpriteImage(
    frameKey: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Fit,
    filterQuality: FilterQuality = FilterQuality.Medium,
    pixelScale: Float = 1f,
    scaleContext: SpriteScaleContext? = null,
) {
    val sprite = LocalSpriteRepository.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, sprite, frameKey) {
        value = withContext(IoDispatcher) { sprite.getImage(frameKey) }
    }
    // 倍率：scaleContext 优先（从设置取），否则回退到显式 pixelScale。
    val appSettings = LocalAppSettings.current
    val effectiveScale = scaleContext?.let { ctx ->
        when (ctx) {
            SpriteScaleContext.Home -> appSettings.homeImageScale
            SpriteScaleContext.Card -> appSettings.cardImageScale
            SpriteScaleContext.DialogBody -> appSettings.dialogBodyImageScale
            SpriteScaleContext.DialogRecipe -> appSettings.dialogRecipeImageScale
            SpriteScaleContext.DialogFavHate -> appSettings.dialogFavHateImageScale
        }
    } ?: pixelScale
    val show = bitmap ?: remember(sprite) { sprite.placeholder() }
    // 严格模式：scaleContext 场景（含 1 倍）与显式 pixelScale > 1 恒按「原图 × 倍率」定尺寸，
    // 不允许回落 fillMaxSize——否则 1 倍会撑满卡片，和 5 倍视觉一样（v42 真机实测 bug）。
    val strict = scaleContext != null || pixelScale > 1f
    if (bitmap == null) {
        // 占位阶段：填满父容器，避免加载时布局跳动。
        Image(
            bitmap = show,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale,
            filterQuality = filterQuality,
        )
    } else if (strict) {
        // 严格整数倍：显示尺寸 = 原图像素 × effectiveScale ÷ 密度（dp），xy 居中于父容器，不填满。
        // 「先乘后除」的意义：先得到目标物理像素（原图 px × 倍率，整数倍=硬边不模糊），
        // 再 ÷ 密度换算成 dp（Compose 的 Image 以 dp 定尺寸、按设备密度铺物理像素）。
        // 用独立 Modifier.size，不叠加调用方的 fillMaxSize，避免尺寸约束被父级填满覆盖；
        // 父容器约束仍会把超出的部分钳制到卡片大小（即「撑满」后再滑大也不变化的原因）。
        // show 已是非空 ImageBitmap（bitmap != null 分支），避免对委托属性做智能转换。
        val scale = LocalDensity.current.density
        val w = Dp(show.width * effectiveScale / scale)
        val h = Dp(show.height * effectiveScale / scale)
        Image(
            bitmap = show,
            contentDescription = contentDescription,
            modifier = Modifier.size(w, h),
            contentScale = contentScale,
            filterQuality = filterQuality,
        )
    } else {
        Image(
            bitmap = show,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
            filterQuality = filterQuality,
        )
    }
}

/** NPC 立绘显示（无需切片，直接解码 `npcs/<id>.png`）；解码同样放到 IO 线程。 */
@Composable
fun NpcPortraitImage(
    npcId: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val sprite = LocalSpriteRepository.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, sprite, npcId) {
        value = withContext(IoDispatcher) { sprite.getNpcImage(npcId) }
    }
    val show = bitmap ?: remember(sprite) { sprite.placeholder() }
    Image(bitmap = show, contentDescription = "NPC $npcId", modifier = modifier, contentScale = contentScale)
}

/** 星级图显示：白(lv_2) / 金(lv_3) / 紫(lv_4)；解码放到 IO 线程。 */
@Composable
fun StarImage(
    level: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val sprite = LocalSpriteRepository.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, sprite, level) {
        value = withContext(IoDispatcher) { sprite.getStarImage(level) }
    }
    val show = bitmap ?: remember(sprite) { sprite.placeholder() }
    Image(bitmap = show, contentDescription = null, modifier = modifier, contentScale = contentScale)
}
