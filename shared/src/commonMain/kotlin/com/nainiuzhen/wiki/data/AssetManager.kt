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

    /** 加载全部资源；[isDebug] 为 true 时展示黑名单物品 / NPC，否则过滤。 */
    suspend fun loadAll(isDebug: Boolean): LoadedData {
        val atlas = buildAtlas()
        val iconMapping = loadIconMapping()
        val itemBlack = loadBlacklist("config/item_blacklist.txt")
        val npcBlack = loadBlacklist("config/npc_blacklist.txt")

        val items = loadItems(atlas, iconMapping, itemBlack, isDebug)
        val recipes = loadRecipes(iconMapping)
        val npcs = loadNpcs(npcBlack, isDebug)
        val schedules = loadSchedules()

        val data = DataRepository(items, recipes, npcs, schedules, itemBlack, npcBlack)
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

    private fun loadIconMapping(): Map<Int, Int> =
        try {
            val text = AssetLoader.loadText("config/icon_mapping.json")
            json.decodeFromString<Map<String, Int>>(text)
                .mapNotNull { (k, v) -> k.toIntOrNull()?.let { it to v } }
                .toMap()
        } catch (_: Exception) {
            emptyMap()
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
        iconMapping: Map<Int, Int>,
        black: Set<Int>,
        isDebug: Boolean,
    ): List<ItemInfo> {
        val raw =
            json.decodeFromString<Map<String, ItemRaw>>(AssetLoader.loadText("config/item_database.json"))
        return raw.values.mapNotNull { r ->
            if (!isDebug && (r.id ?: 0) in black) return@mapNotNull null
            val mapped = iconMapping[r.id ?: 0]
            val frameKey = (mapped ?: r.icon ?: r.id).toString()
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

    private fun loadRecipes(iconMapping: Map<Int, Int>): List<RecipeInfo> {
        val raw =
            json.decodeFromString<CompoundRaw>(AssetLoader.loadText("config/compound_unlocks.json"))
        return raw.items.mapNotNull { r ->
            val mapped = iconMapping[r.id ?: 0]
            val frameKey = (mapped ?: r.icon ?: r.id).toString()
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
