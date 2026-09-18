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
 *
 * ## ⚠️ 不要在整份 plist 文本上迭代正则（2026-09-18 安卓冷启动 11 秒的真凶）
 *
 * 本解析器原先用 `frameBlockRegex.findAll(plist)` 逐帧迭代。Kotlin/JVM 的
 * `Regex.findAll` 每取一个匹配都走 `matchAt(input, index)` → `Pattern.matcher(input)` →
 * `new Matcher(input)`；**Android 的 `java.util.regex` 由 ICU 实现，`Matcher.reset(input)`
 * 会把整份输入 memmove 进 native 缓冲**（栈底就是 `libicu_jni.so MatcherState::updateInput → memmove`）。
 * 于是 26 张 plist 共 6565 个 `<key>xxx.png</key>` 帧块，会把 3 MB 文本反复搬运约 3.7 GB：
 * 实测该 IO 线程烧掉 **5.4 s CPU**，冷启动总耗时 11.0 s（AOT 编译与否毫无差别，因为瓶颈是
 * native memmove 而非字节码解释）；同一份正则在桌面 JVM 只要 70 ms（OpenJDK 的 Matcher 只持有
 * CharSequence 引用、不复制）。iOS 侧 `Regex` 不走 ICU 这条路径，所以 iOS 一直很快。
 *
 * 因此这里把「块级扫描」改成纯 `indexOf` 手写扫描（与旧正则语义逐条等价，已用 26 张真实 plist
 * 校验：帧集合与 11 个字段值 6565/6565 完全一致）；**5 个取值正则保留不变，但只作用于单帧
 * `<dict>` 片段（约 400 字符）**，单次调用的 memmove 从 1.3 MB 降到 400 B，总计约 13 MB，
 * 解析耗时从 ~10 s 降到 ~30 ms（设备实测手写扫描版 24 ms）。
 *
 * 若日后还要动这里：**任何「对整份 plist 文本调用 Kotlin Regex 取值」的写法都会重新引入
 * 这个 O(帧数 × 文本长度) 的搬运开销**，务必保持「块级手写扫描 + 片段内正则/数字扫描」的结构。
 */
object PlistParser {
    // ---- 帧块扫描不再使用正则，改为 indexOf 手写扫描（见类注释） ----
    /** 帧块起始标记。 */
    private const val KEY_OPEN = "<key>"
    /** key 结束标记。 */
    private const val KEY_CLOSE = "</key>"
    /** 帧块字典起始标记。 */
    private const val DICT_OPEN = "<dict>"
    /** 帧块字典结束标记。 */
    private const val DICT_CLOSE = "</dict>"
    /** 帧名后缀（plist 里带后缀，索引键不带）。 */
    private const val PNG_SUFFIX = ".png"

    // ---- 以下取值正则只作用于「单帧 <dict> 片段」，不得改回整份文本 ----
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
        val len = plist.length
        // 扫描游标：始终指向「上一帧块 </dict> 之后」或「刚跳过的 key 之后」，
        // 与旧正则 findAll 从上一匹配末尾继续的推进方式一致（帧内的 <key>frame</key>
        // 等不会被当成新的帧块）。
        var cursor = 0
        while (cursor < len) {
            val keyOpen = plist.indexOf(KEY_OPEN, cursor)
            if (keyOpen < 0) break
            val contentStart = keyOpen + KEY_OPEN.length
            val keyClose = plist.indexOf(KEY_CLOSE, contentStart)
            if (keyClose < 0) break
            val nameEnd = keyClose - PNG_SUFFIX.length
            // 等价于旧正则的 `<key>([^<]+\.png)</key>`：
            //   ① key 内容不含 '<'（首个 '<' 就是 </key> 自己）；
            //   ② 内容至少 1 个字符（nameEnd 之外还要有字符）且以 .png 结尾。
            if (plist.indexOf('<', contentStart) != keyClose ||
                nameEnd <= contentStart ||
                !plist.startsWith(PNG_SUFFIX, nameEnd)
            ) {
                cursor = keyClose + KEY_CLOSE.length
                continue
            }
            val name = plist.substring(contentStart, nameEnd)
            // 等价于旧正则的 `</key>\s*<dict>`。
            var dictOpen = keyClose + KEY_CLOSE.length
            while (dictOpen < len && plist[dictOpen].isWhitespace()) dictOpen++
            if (!plist.startsWith(DICT_OPEN, dictOpen)) {
                cursor = keyClose + KEY_CLOSE.length
                continue
            }
            val dictStart = dictOpen + DICT_OPEN.length
            val dictEnd = plist.indexOf(DICT_CLOSE, dictStart)
            if (dictEnd < 0) break
            cursor = dictEnd + DICT_CLOSE.length

            // 只在这 400 字符左右的片段上跑取值正则（见类注释：切勿改回整份文本）。
            val dict = plist.substring(dictStart, dictEnd)
            val rectMatch = frameRectRegex.find(dict) ?: continue
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
            // 帧名在 plist 中带 .png 后缀；统一去掉，使 atlas 索引键与
            // AssetManager 推导出的 frameKey（纯数字 id，不含后缀）一致。
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
