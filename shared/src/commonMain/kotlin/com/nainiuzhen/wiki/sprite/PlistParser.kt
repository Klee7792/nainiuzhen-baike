package com.nainiuzhen.wiki.sprite

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import com.nainiuzhen.wiki.data.model.SpriteAtlasFrame

/**
 * Cocos2d plist 图集解析器（纯 Kotlin，跨平台）。
 *
 * 解析 `frames` 字典下每个 `<key>NAME.png</key>` 对应的帧：
 * `<key>frame</key><string>{{x,y},{w,h}}</string>`、
 * `<key>rotated</key><true|false/>`、
 * `<key>offset</key><string>{x,y}</string>`（旧格式，保留）、
 * `<key>sourceColorRect</key><string>{{x,y},{w,h}}</string>`（新格式，贴回画布的偏移）、
 * `<key>sourceSize</key><string>{w,h}</string>`。
 *
 * 帧的矩形统一以 `IntRect(left=x, top=y, right=x+w, bottom=y+h)` 存储，
 * 供切片器取 `left/top` 与宽高；`sourceColorRect` 的 `x,y` 存入 [SpriteAtlasFrame.sourceColorRect]。
 */
object PlistParser {
    private val frameBlockRegex =
        Regex("<key>([^<]+\\.png)</key>\\s*<dict>(.*?)</dict>", RegexOption.DOT_MATCHES_ALL)
    private val frameRectRegex =
        Regex("<key>frame</key>\\s*<string>\\{\\{(\\d+),(\\d+)\\},\\{(\\d+),(\\d+)\\}\\}</string>")
    private val rotatedRegex = Regex("<key>rotated</key>\\s*<true/>")
    private val offsetRegex =
        Regex("<key>offset</key>\\s*<string>\\{([-\\d]+),([-\\d]+)\\}</string>")
    private val sourceColorRectRegex =
        Regex("<key>sourceColorRect</key>\\s*<string>\\{\\{(\\d+),(\\d+)\\},\\{(\\d+),(\\d+)\\}\\}</string>")
    private val sourceSizeRegex =
        Regex("<key>sourceSize</key>\\s*<string>\\{(\\d+),(\\d+)\\}</string>")

    /**
     * 解析单个 plist 文本为 `<帧名, 帧>` 映射。
     *
     * @param plist plist 文本内容。
     * @param sheetName 所属图集名称（写到每帧的 [SpriteAtlasFrame.sheetName]）。
     */
    fun parse(plist: String, sheetName: String): Map<String, SpriteAtlasFrame> {
        val result = mutableMapOf<String, SpriteAtlasFrame>()
        frameBlockRegex.findAll(plist).forEach { match ->
            // 帧名在 plist 中带 .png 后缀；统一去掉，使 atlas 索引键与
            // AssetManager 推导出的 frameKey（纯数字 id，不含后缀）一致。
            val name = match.groupValues[1].removeSuffix(".png")
            val dict = match.groupValues[2]
            val rectMatch = frameRectRegex.find(dict) ?: return@forEach
            val (x, y, w, h) = rectMatch.destructured
            val rotated = rotatedRegex.containsMatchIn(dict)
            val offsetMatch = offsetRegex.find(dict)
            val ox = offsetMatch?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
            val oy = offsetMatch?.groupValues?.getOrNull(2)?.toIntOrNull() ?: 0
            val colorRectMatch = sourceColorRectRegex.find(dict)
            val cx = colorRectMatch?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
            val cy = colorRectMatch?.groupValues?.getOrNull(2)?.toIntOrNull() ?: 0
            val sizeMatch = sourceSizeRegex.find(dict)
            val sw = sizeMatch?.groupValues?.getOrNull(1)?.toIntOrNull() ?: w.toInt()
            val sh = sizeMatch?.groupValues?.getOrNull(2)?.toIntOrNull() ?: h.toInt()
            result[name] = SpriteAtlasFrame(
                sheetName = sheetName,
                rect = IntRect(x.toInt(), y.toInt(), x.toInt() + w.toInt(), y.toInt() + h.toInt()),
                rotated = rotated,
                offset = IntOffset(ox, oy),
                sourceSize = IntSize(sw, sh),
                sourceColorRect = IntOffset(cx, cy),
            )
        }
        return result
    }
}
