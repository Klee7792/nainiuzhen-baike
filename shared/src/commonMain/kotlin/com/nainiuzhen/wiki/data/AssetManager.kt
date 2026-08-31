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

    /** 加载全部资源；[isDebug] 为 true 时展示黑名单物品 / 配方 / NPC，否则过滤。 */
    suspend fun loadAll(isDebug: Boolean): LoadedData {
        val atlas = buildAtlas()
        val iconMapping = loadIconMapping()
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
        val npcBlack = loadBlacklist("config/npc_blacklist.txt")
        val recipeBlack = loadBlacklist("config/recipe_blacklist.txt")

        val items = loadItems(atlas, iconMapping, itemBlack, isDebug)
        val recipes = loadRecipes(atlas, iconMapping, recipeBlack, isDebug)
        val npcs = loadNpcs(npcBlack, isDebug)
        val schedules = loadSchedules()

        val data = DataRepository(items, recipes, npcs, schedules, itemBlack, npcBlack, recipeBlack)
        val sprite = SpriteRepository(atlas, slicer, cache)
        return LoadedData(data, sprite)
    }

    /** 构建跨图集的帧索引。 */
    private fun buildAtlas(): SpriteAtlas {
        val sheetNames =
            buildList {
                add("items")
                for (i in 1..22) add("items$i")
                add("equip")
                add("headwear")
                add("hats_empty")
            }
        val map = mutableMapOf<String, SpriteAtlasFrame>()
        for (sheet in sheetNames) {
            try {
                val plist = AssetLoader.loadText("res/$sheet.plist")
                PlistParser.parse(plist, sheet).forEach { (k, v) -> map[k] = v }
            } catch (_: Exception) {
                // 该图集不存在则跳过
            }
        }
        return SpriteAtlas(map)
    }

    private fun loadIconMapping(): Map<Int, String> =
        try {
            val text = AssetLoader.loadText("config/icon_mapping.json")
            json.decodeFromString<Map<String, JsonElement>>(text)
                .mapNotNull { (k, v) ->
                    val id = k.toIntOrNull() ?: return@mapNotNull null
                    // 值可能是数字（如 40000107）或带后缀的字符串（如 "40000107_time"），统一取字符串内容。
                    id to v.jsonPrimitive.content
                }
                .toMap()
        } catch (_: Exception) {
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
        } catch (_: Exception) {
            emptySet()
        }

    private fun extractCategory(desc: String): String {
        val match = Regex("类型：([^/#]+)").find(desc)
        return match?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() } ?: "其他"
    }

    private fun loadItems(
        atlas: SpriteAtlas,
        iconMapping: Map<Int, String>,
        black: Set<Int>,
        isDebug: Boolean,
    ): List<ItemInfo> {
        val raw =
            json.decodeFromString<Map<String, ItemRaw>>(AssetLoader.loadText("config/item_database.json"))
        return raw.values.mapNotNull { r ->
            // 过滤：非 debug 包剔除黑名单 id（清单见上方 loadBlacklist，人工维护，勿自动还原）
            if (!isDebug && (r.id ?: 0) in black) return@mapNotNull null
            val mapped = iconMapping[r.id ?: 0]
            val candidate = mapped ?: r.icon?.toString() ?: (r.id ?: 0).toString()
            val frameKey = resolveFrameKey(atlas, candidate)
            val category = extractCategory(r.desc)
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
    }

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
