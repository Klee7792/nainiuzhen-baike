package com.nainiuzhen.wiki.data

import androidx.compose.ui.unit.IntSize
import com.nainiuzhen.wiki.data.model.ItemGroup
import com.nainiuzhen.wiki.data.model.ItemInfo
import com.nainiuzhen.wiki.data.model.NpcInfo
import com.nainiuzhen.wiki.data.model.NpcSchedule
import com.nainiuzhen.wiki.data.model.RecipeInfo
import com.nainiuzhen.wiki.data.model.RecipeMaterial
import com.nainiuzhen.wiki.data.model.ScenePoint
import com.nainiuzhen.wiki.data.model.SpriteAtlas
import com.nainiuzhen.wiki.data.model.SpriteAtlasFrame
import com.nainiuzhen.wiki.data.repository.DataRepository
import com.nainiuzhen.wiki.data.repository.SpriteCacheManager
import com.nainiuzhen.wiki.data.repository.SpriteRepository
import com.nainiuzhen.wiki.data.source.AssetLoader
import com.nainiuzhen.wiki.data.source.SpriteSlicer
import com.nainiuzhen.wiki.sprite.PlistParser
import com.nainiuzhen.wiki.utils.AppLog
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive

/**
 * 资源管理器：在启动时读取全部 JSON / 图集，构建 [DataRepository] 与 [SpriteRepository]。
 *
 * @param slicer 平台切片器（Android 注入）。
 * @param cache 平台缓存管理器（Android 注入）。
 */
/**
 * 「准备数据」阶段的总步数，口径 = 26 张 plist（items + items1..22 + equip + headwear + hats_empty）
 * + 1 次 icon_mapping + 3 份黑名单 + 4 份业务数据（物品 / 配方 / NPC / 日程）= 34。
 * 与 [AssetManager.loadAll] 里 `tick()` 的次数必须一致，改任一侧都要同步另一侧。
 */
private const val DATA_LOAD_STEP_COUNT = 34

class AssetManager(
    private val slicer: SpriteSlicer,
    private val cache: SpriteCacheManager,
) {
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            // 数据里存在显式 null（如 compound_unlocks.json 的 target/targetNum），
            // 关闭 explicitNulls 让 null 走字段默认值，避免 JsonDecodingException 崩溃。
            explicitNulls = false
        }

    /**
     * 加载全部资源；[isDebug] 为 true 时展示黑名单物品 / 配方 / NPC，否则过滤。
     *
     * [onProgress] 逐「步」回调 `(done, total)`，供启动加载页显示真实百分比 —— 这一阶段
     * （读 26 张 plist + 解析物品 / 配方 / NPC / 日程）此前完全没有进度回流，加载页会
     * 长时间卡在 0%，故补上。步数口径见 [DATA_LOAD_STEP_COUNT]。
     */
    suspend fun loadAll(
        isDebug: Boolean,
        onProgress: suspend (done: Int, total: Int) -> Unit = { _, _ -> },
    ): LoadedData {
        var done = 0
        // 每完成一步就回调一次（回调在调用方线程，由调用方切主线程更新状态）。
        suspend fun tick() {
            done += 1
            onProgress(done, DATA_LOAD_STEP_COUNT)
        }
        val atlas = buildAtlas { tick() }
        val iconMapping = loadIconMapping()
        tick()
        // ⚠️ item_blacklist.txt 为【人工维护】清单，非脚本自动生成，切勿用 item_database.json
        // 全量或任何规则重新生成覆盖，否则会丢失下列手工追加项：
        //   规则并集 = name 含(作废)/(无用)/(废弃)/(测试)/(测试用) ∪ desc 含 拼接场景使用/不进背包/不需要翻译 ∪ icon=260000
        //   + 手工追加 = 现有黑名单中未被规则覆盖的 23 个、27XXXX 名称含「预留」的 42 个、id 9
        // 运行时读取的是 app/src/main/assets/config/ 副本（composeResources/files/ 那份是同步的死副本，也要一并改）。
        // 调整入口：assets/config/item_blacklist.txt（由 extract_blacklist2.py 生成 blacklist2.json 后回写两处 txt）。
        // recipe_blacklist.txt 为【人工维护】清单（由 extract_recipe_blacklist.py 生成），同样勿用脚本全量覆盖：
        //   规则并集 = 某配方 target(产物 id) ∈ 物品黑名单(1200) ∪ 配方自身 icon==260000
        //   运行时读取 app/src/main/assets/config/ 副本（composeResources/files/ 那份是同步的死副本，也要一并改）。
        val itemBlack = loadBlacklist("config/item_blacklist.txt")
        tick()
        val npcBlack = loadBlacklist("config/npc_blacklist.txt")
        tick()
        val recipeBlack = loadBlacklist("config/recipe_blacklist.txt")
        tick()

        // 四份业务数据逐项留痕：**成功也要记**（条目数 0 = 素材没真正加载出来，
        // 却不会抛异常，是最难查的一类故障）。
        val items = loadItems(atlas, iconMapping, itemBlack, isDebug).logCount("物品")
        tick()
        val recipes = loadRecipes(atlas, iconMapping, recipeBlack, isDebug).logCount("配方")
        tick()
        val npcs = loadNpcs(npcBlack, isDebug).logCount("NPC")
        tick()
        val schedules = loadSchedules()
        AppLog.i("日程载入 ${schedules.size} 个 NPC")
        if (schedules.isEmpty()) AppLog.w("日程数据为空")
        tick()

        val data = DataRepository(items, recipes, npcs, schedules, itemBlack, npcBlack, recipeBlack)
        val sprite = SpriteRepository(atlas, slicer, cache)
        return LoadedData(data, sprite)
    }

    /**
     * 构建跨图集的帧索引。每解析完一张 plist 回调一次 [onSheet]（供启动进度计数）。
     */
    private suspend fun buildAtlas(onSheet: suspend () -> Unit): SpriteAtlas {
        val sheetNames =
            buildList {
                add("items")
                for (i in 1..22) add("items$i")
                add("equip")
                add("headwear")
                add("hats_empty")
            }
        val map = mutableMapOf<String, SpriteAtlasFrame>()
        var emptySheets = 0
        for (sheet in sheetNames) {
            try {
                val plist = AssetLoader.loadText("res/$sheet.plist")
                val parsed = PlistParser.parse(plist, sheet)
                parsed.forEach { (k, v) -> map[k] = v }
                if (parsed.isEmpty()) {
                    emptySheets += 1
                    AppLog.w("图集 $sheet 解析结果为空（plist 为空或格式不符）")
                }
            } catch (t: Throwable) {
                // 该图集不存在 / 读取失败则跳过 —— 但必须留痕：
                // 「全部跳过」会让图集索引为 0，表现为整页空白且无任何报错。
                AppLog.e("图集加载失败: res/$sheet.plist", t)
            }
            onSheet()
        }
        AppLog.i("图集索引构建完成：${map.size} 帧 / 空图集 $emptySheets 张")
        if (map.isEmpty()) AppLog.e("图集索引为 0（全部 plist 缺失或解析失败）")
        return SpriteAtlas(map)
    }

    private fun loadIconMapping(): Map<Int, String> =
        try {
            AssetLoader.loadText("config/icon_mapping.json")
                .let { text ->
                    json.decodeFromString<Map<String, JsonElement>>(text)
                        .mapNotNull { (k, v) ->
                            val id = k.toIntOrNull() ?: return@mapNotNull null
                            // 值可能是数字（如 40000107）或带后缀的字符串（如 "40000107_time"），统一取字符串内容。
                            id to v.jsonPrimitive.content
                        }
                        .toMap()
                }
                .also { mapping ->
                    AppLog.i("icon_mapping 载入 ${mapping.size} 条")
                    if (mapping.isEmpty()) AppLog.w("icon_mapping 为空（文件缺失或格式不符）")
                }
        } catch (t: Throwable) {
            AppLog.e("icon_mapping 加载失败: config/icon_mapping.json", t)
            emptyMap()
        }

    /**
     * 把候选帧名解析为图集中真实存在的帧：先精确匹配；缺失时依次尝试 `_time` / `_view` 后缀
     * （部分物品 icon 用基础 id 表示，真实切片帧带后缀，例如 40000107 → 40000107_time）。
     * 仍找不到则原样返回，交由 [SpriteRepository.getImage] 以占位图兜底。
     */
    private fun resolveFrameKey(atlas: SpriteAtlas, candidate: String): String {
        if (atlas.getFrame(candidate) != null) return candidate
        for (suffix in listOf("_time", "_view")) {
            val withSuffix = "$candidate$suffix"
            if (atlas.getFrame(withSuffix) != null) return withSuffix
        }
        return candidate
    }

    private fun loadBlacklist(name: String): Set<Int> =
        try {
            AssetLoader.loadText(name)
                .lineSequence()
                .mapNotNull { it.trim().toIntOrNull() }
                .toSet()
                .also { set ->
                    AppLog.i("黑名单 $name = ${set.size} 条")
                    if (set.isEmpty()) AppLog.w("黑名单 $name 为空（文件缺失或内容异常）")
                }
        } catch (t: Throwable) {
            AppLog.e("黑名单加载失败: $name", t)
            emptySet()
        }

    private fun extractCategory(desc: String): String {
        val match = Regex("类型：([^/#]+)").find(desc)
        return match?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() } ?: "其他"
    }

    /**
     * 类别归一化（变更点 #27-B）：在 [extractCategory] 之后再做一次性字符串合并，
     * 把分散的同义 / 细分类别归并到统一标签：
     *  ① 其他 & 其它 → 其他
     *  ② XX皮肤 & 皮肤 → 皮肤（所有以「皮肤」结尾的标签，如 主房皮肤 / 床皮肤 / 信箱皮肤 …）
     *  ③ 各种鱼类 → 鱼类（河鱼 / 海鱼 / 湖鱼 / 珍稀鱼类 及其「低级」变体）
     * 注意：鱼饵、鱼食 虽含「鱼」字但不属于鱼类，不在此合并。
     */
    private val FISH_CATEGORY_KEYWORDS = listOf("鱼类", "河鱼", "海鱼", "湖鱼", "珍稀鱼类")
    private fun normalizeCategory(raw: String): String {
        var c = raw
        if (c == "其它") c = "其他"
        if (c.endsWith("皮肤")) c = "皮肤"
        if (FISH_CATEGORY_KEYWORDS.any { c.contains(it) }) c = "鱼类"
        return c
    }

    private fun loadItems(
        atlas: SpriteAtlas,
        iconMapping: Map<Int, String>,
        black: Set<Int>,
        isDebug: Boolean,
    ): List<ItemInfo> {
        val raw =
            json.decodeFromString<Map<String, ItemRaw>>(AssetLoader.loadText("config/item_database.json"))
        val parsed = raw.values.mapNotNull { r ->
            // 过滤：非 debug 包剔除黑名单 id（清单见上方 loadBlacklist，人工维护，勿自动还原）
            if (!isDebug && (r.id ?: 0) in black) return@mapNotNull null
            val mapped = iconMapping[r.id ?: 0]
            val candidate = mapped ?: r.icon?.toString() ?: (r.id ?: 0).toString()
            val frameKey = resolveFrameKey(atlas, candidate)
            val category = normalizeCategory(extractCategory(r.desc)) // #27-B 一次性归一化
            val group =
                when (atlas.sheetFor(frameKey)) {
                    "equip" -> ItemGroup.Equip
                    "headwear" -> ItemGroup.Headwear
                    else -> ItemGroup.Items
                }
            ItemInfo(
                id = r.id ?: 0,
                name = r.name,
                descRaw = r.desc,
                source = r.source,
                iconHint = r.icon,
                type = r.type ?: 0,
                price = r.price,
                sellboxPrice = r.sellboxPrice,
                iconFrameKey = frameKey,
                categoryLabel = category,
                group = group,
            )
        }
        // #27-B 规则④：合并后仅剩 1 个物品的类别并入「其他」（如 书籍 / 传说道具 / 发型 …）。
        // 合并只会增大类别规模，故此处仍处 singleton 的必是原 count==1 且未被吸收的类别。
        val counts = parsed.groupingBy { it.categoryLabel }.eachCount()
        return parsed.map { item ->
            if ((counts[item.categoryLabel] ?: 0) <= 1) item.copy(categoryLabel = "其他") else item
        }
    }

    /**
     * 解析配方（`compound_unlocks.json`）。
     *
     * ## ⚠️ 配方主图只认 `icon`，**不要**改回「按产物 `target` 取图」（勿删！）
     *
     * 源数据 `icon` 字段有**成批错误**：共 **89 条**配方的 `icon` 被写成 `260415`
     * （物品 id 260415 的名字就是「士力架」）。其中 `360114 士力架食谱` 的 `target` 也是
     * `260415`（本来就对），其余 **88 条**是错的。
     *
     * 第一版修法是在**代码里**改成按产物 `target` 取帧，结果引入新 bug：
     * `360450 骑士之剑图纸` / `360451 野兽之爪图纸` / `360452 攻击药水配方` /
     * `360453 防御药水配方` / `360454 暴击药水配方` / `360455 武装战锤图纸` /
     * `360456 狂化战戒图纸` 这 **7 条**全部显示成「木剑」—— 因为它们的 `target` 是占位值
     * `40000202`（木剑），`materials` 也全是占位（干草 / 纤维 / 木头），属**未实装**配方。
     * ⇒ **`target` 不是权威字段**，这 7 条的 `target` 是脏数据。
     *
     * 最终方案是**数据侧修、代码侧只认 `icon`**：已把那 88 条的 `icon` 直接改成各自的
     * `target` 值（改的是私有素材仓 `nainiuzhen-assets/config/compound_unlocks.json`，
     * 2026-09-19），本函数仍只走 `icon_mapping` → `icon` → `id` 这条链路。
     *
     * 可行性依据（已核实）：`icon_mapping.json` 里**没有任何 36xxxx（配方 id）键**（0 条），
     * 因此配方的 `icon` 字段就是最终决定项，改配置必定生效。
     */
    private fun loadRecipes(
        atlas: SpriteAtlas,
        iconMapping: Map<Int, String>,
        black: Set<Int>,
        isDebug: Boolean,
    ): List<RecipeInfo> {
        val raw =
            json.decodeFromString<CompoundRaw>(AssetLoader.loadText("config/compound_unlocks.json"))
        return raw.items.mapNotNull { r ->
            // 过滤：非 debug 包剔除黑名单 id（清单见上方 loadBlacklist，人工维护，勿自动还原）
            if (!isDebug && (r.id ?: 0) in black) return@mapNotNull null
            val mapped = iconMapping[r.id ?: 0]
            val candidate = mapped ?: r.icon?.toString() ?: (r.id ?: 0).toString()
            val frameKey = resolveFrameKey(atlas, candidate)
            RecipeInfo(
                id = r.id ?: 0,
                name = r.name,
                type = r.type ?: 0,
                typeLabel = r.typeLabel,
                category = r.category,
                target = r.target ?: 0,
                targetNum = r.targetNum ?: 0,
                materials = r.materials.map { RecipeMaterial(it.id ?: 0, it.num ?: 0) },
                iconHint = r.icon ?: (r.id ?: 0),
                deblockingDesc = r.deblockingDesc,
                descRaw = r.desc,
                price = r.price,
                iconFrameKey = frameKey,
            )
        }
    }

    private fun loadNpcs(black: Set<Int>, isDebug: Boolean): List<NpcInfo> {
        val raw =
            json.decodeFromString<Map<String, NpcRaw>>(AssetLoader.loadText("config/npc_database.json"))
        return raw.values.mapNotNull { r ->
            if (!isDebug && (r.id ?: 0) in black) return@mapNotNull null
            NpcInfo(
                id = r.id ?: 0,
                name = r.name,
                descRaw = r.desc,
                address = r.address,
                birthday = r.birthday,
                sex = r.sex ?: 0,
                loveItemId = r.loveItemId ?: 0,
                maxStar = r.maxStar ?: 0,
                likeItems = r.likeItems,
                hateItems = r.hateItems,
                bestFavorItems = r.bestFavorItems,
            )
        }
    }

    private fun loadSchedules(): Map<Int, List<NpcSchedule>> {
        val raw =
            json.decodeFromString<Map<String, NpcAiRaw>>(AssetLoader.loadText("config/npc_ai_database.json"))
        val result = mutableMapOf<Int, MutableList<NpcSchedule>>()
        raw.values.forEach { ai ->
            ai.schedules.forEach { s ->
                val schedule =
                    NpcSchedule(
                        id = s.id ?: 0,
                        npcId = s.npcId ?: 0,
                        name = s.name,
                        startTime = s.startTime ?: 0,
                        startTimeText = s.startTimeText,
                        startPoint =
                            ScenePoint(
                                s.startPoint.sceneId,
                                s.startPoint.row,
                                s.startPoint.col,
                                s.startPoint.sceneName,
                            ),
                        endPoint =
                            ScenePoint(
                                s.endPoint.sceneId,
                                s.endPoint.row,
                                s.endPoint.col,
                                s.endPoint.sceneName,
                            ),
                        week = s.week,
                        season = s.season,
                        weather = s.weather,
                        isAstar = s.isAstar ?: 0,
                        relation = s.relation,
                    )
                result.getOrPut(s.npcId ?: 0) { mutableListOf() }.add(schedule)
            }
        }
        return result
    }

    /**
     * 把「加载结果条目数」写进诊断日志：成功也记，为 0 时升级为警告。
     * 保持返回类型不变（[C]），调用方可直接串在加载表达式后面。
     */
    private fun <T> List<T>.logCount(label: String): List<T> {
        AppLog.i("$label 载入 $size 条")
        if (isEmpty()) AppLog.w("$label 数据为 0 条（素材未加载或被黑名单全部过滤）")
        return this
    }

    /** 加载结果聚合。 */
    data class LoadedData(
        val data: DataRepository,
        val sprite: SpriteRepository,
    )
}

// ---- 解析用的原始 DTO（仅本文件可见）----

@Serializable
private data class ItemRaw(
    val id: Int? = null,
    val name: String,
    val desc: String = "",
    val source: String? = null,
    val icon: Int? = null,
    val type: Int? = null,
    val price: Int? = null,
    @SerialName("sellbox_price") val sellboxPrice: Int? = null,
)

@Serializable
private data class CompoundRaw(
    val summary: JsonElement? = null,
    val items: List<RecipeRaw> = emptyList(),
)

@Serializable
private data class RecipeRaw(
    val id: Int? = null,
    val name: String,
    val type: Int? = null,
    val typeName: String? = null,
    val typeLabel: String = "",
    val category: String = "",
    val target: Int? = null,
    val targetNum: Int? = null,
    val materials: List<MaterialRaw> = emptyList(),
    val icon: Int? = null,
    val price: Int? = null,
    val deblockingDesc: String? = null,
    val desc: String? = null,
)

@Serializable
private data class MaterialRaw(
    val id: Int? = null,
    val num: Int? = null,
)

@Serializable
private data class NpcRaw(
    val id: Int? = null,
    val name: String,
    val desc: String = "",
    val address: String = "",
    val birthday: String = "",
    val sex: Int? = null,
    val loveItemId: Int? = null,
    val maxStar: Int? = null,
    val likeItems: List<Int> = emptyList(),
    val hateItems: List<Int> = emptyList(),
    val bestFavorItems: List<Int> = emptyList(),
)

@Serializable
private data class NpcAiRaw(
    val npcId: Int? = null,
    val npcName: String = "",
    val schedules: List<ScheduleRaw> = emptyList(),
)

@Serializable
private data class ScheduleRaw(
    val id: Long? = null,
    val npcId: Int? = null,
    val name: String = "",
    val startTime: Int? = null,
    val startTimeText: String = "",
    val startPoint: ScenePointRaw = ScenePointRaw(),
    val endPoint: ScenePointRaw = ScenePointRaw(),
    val week: List<Int> = emptyList(),
    val season: List<Int> = emptyList(),
    val weather: List<Int> = emptyList(),
    val isAstar: Int? = null,
    val relation: List<Int> = emptyList(),
)

@Serializable
private data class ScenePointRaw(
    val sceneId: Int = 0,
    val row: Int = 0,
    val col: Int = 0,
    val sceneName: String = "",
)
