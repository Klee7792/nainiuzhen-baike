package com.harvesttown.encyclopedia.data

import androidx.compose.ui.unit.IntSize
import com.harvesttown.encyclopedia.data.model.ItemGroup
import com.harvesttown.encyclopedia.data.model.ItemInfo
import com.harvesttown.encyclopedia.data.model.NpcInfo
import com.harvesttown.encyclopedia.data.model.NpcSchedule
import com.harvesttown.encyclopedia.data.model.RecipeInfo
import com.harvesttown.encyclopedia.data.model.RecipeMaterial
import com.harvesttown.encyclopedia.data.model.ScenePoint
import com.harvesttown.encyclopedia.data.model.SpriteAtlas
import com.harvesttown.encyclopedia.data.model.SpriteAtlasFrame
import com.harvesttown.encyclopedia.data.repository.DataRepository
import com.harvesttown.encyclopedia.data.repository.SpriteCacheManager
import com.harvesttown.encyclopedia.data.repository.SpriteRepository
import com.harvesttown.encyclopedia.data.source.AssetLoader
import com.harvesttown.encyclopedia.data.source.SpriteSlicer
import com.harvesttown.encyclopedia.sprite.PlistParser
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
        }

    /** 加载全部资源；[isDebug] 为 true 时展示黑名单物品 / NPC，否则过滤。 */
    suspend fun loadAll(isDebug: Boolean): LoadedData {
        val atlas = buildAtlas()
        val iconMapping = loadIconMapping()
        val itemBlack = loadBlacklist("item_blacklist.txt")
        val npcBlack = loadBlacklist("npc_blacklist.txt")

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
                val plist = AssetLoader.loadText("$sheet.plist")
                PlistParser.parse(plist, sheet).forEach { (k, v) -> map[k] = v }
            } catch (_: Exception) {
                // 该图集不存在则跳过
            }
        }
        return SpriteAtlas(map)
    }

    private fun loadIconMapping(): Map<Int, Int> =
        try {
            val text = AssetLoader.loadText("icon_mapping.json")
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
            json.decodeFromString<Map<String, ItemRaw>>(AssetLoader.loadText("item_database.json"))
        return raw.values.mapNotNull { r ->
            if (!isDebug && r.id in black) return@mapNotNull null
            val mapped = iconMapping[r.id]
            val frameKey = (mapped ?: r.icon ?: r.id).toString()
            val category = extractCategory(r.desc)
            val group =
                when (atlas.sheetFor(frameKey)) {
                    "equip" -> ItemGroup.Equip
                    "headwear" -> ItemGroup.Headwear
                    else -> ItemGroup.Items
                }
            ItemInfo(
                id = r.id,
                name = r.name,
                descRaw = r.desc,
                source = r.source,
                iconHint = r.icon,
                type = r.type,
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
            json.decodeFromString<CompoundRaw>(AssetLoader.loadText("compound_unlocks.json"))
        return raw.items.mapNotNull { r ->
            val mapped = iconMapping[r.id]
            val frameKey = (mapped ?: r.icon ?: r.id).toString()
            RecipeInfo(
                id = r.id,
                name = r.name,
                type = r.type,
                typeLabel = r.typeLabel,
                category = r.category,
                target = r.target,
                targetNum = r.targetNum,
                materials = r.materials.map { RecipeMaterial(it.id, it.num) },
                iconHint = r.icon ?: r.id,
                deblockingDesc = r.deblockingDesc,
                descRaw = r.desc,
                price = r.price,
                iconFrameKey = frameKey,
            )
        }
    }

    private fun loadNpcs(black: Set<Int>, isDebug: Boolean): List<NpcInfo> {
        val raw =
            json.decodeFromString<Map<String, NpcRaw>>(AssetLoader.loadText("npc_database.json"))
        return raw.values.mapNotNull { r ->
            if (!isDebug && r.id in black) return@mapNotNull null
            NpcInfo(
                id = r.id,
                name = r.name,
                descRaw = r.desc,
                address = r.address,
                birthday = r.birthday,
                sex = r.sex,
                loveItemId = r.loveItemId,
                maxStar = r.maxStar,
                likeItems = r.likeItems,
                hateItems = r.hateItems,
                bestFavorItems = r.bestFavorItems,
            )
        }
    }

    private fun loadSchedules(): Map<Int, List<NpcSchedule>> {
        val raw =
            json.decodeFromString<Map<String, NpcAiRaw>>(AssetLoader.loadText("npc_ai_database.json"))
        val result = mutableMapOf<Int, MutableList<NpcSchedule>>()
        raw.values.forEach { ai ->
            ai.schedules.forEach { s ->
                val schedule =
                    NpcSchedule(
                        id = s.id,
                        npcId = s.npcId,
                        name = s.name,
                        startTime = s.startTime,
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
                        isAstar = s.isAstar,
                        relation = s.relation,
                    )
                result.getOrPut(s.npcId) { mutableListOf() }.add(schedule)
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
    val id: Int,
    val name: String,
    val desc: String = "",
    val source: String? = null,
    val icon: Int? = null,
    val type: Int = 0,
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
    val id: Int,
    val name: String,
    val type: Int = 0,
    val typeName: String? = null,
    val typeLabel: String = "",
    val category: String = "",
    val target: Int = 0,
    val targetNum: Int = 0,
    val materials: List<MaterialRaw> = emptyList(),
    val icon: Int? = null,
    val price: Int? = null,
    val deblockingDesc: String? = null,
    val desc: String? = null,
)

@Serializable
private data class MaterialRaw(
    val id: Int,
    val num: Int,
)

@Serializable
private data class NpcRaw(
    val id: Int,
    val name: String,
    val desc: String = "",
    val address: String = "",
    val birthday: String = "",
    val sex: Int = 0,
    val loveItemId: Int = 0,
    val maxStar: Int = 0,
    val likeItems: List<Int> = emptyList(),
    val hateItems: List<Int> = emptyList(),
    val bestFavorItems: List<Int> = emptyList(),
)

@Serializable
private data class NpcAiRaw(
    val npcId: Int,
    val npcName: String = "",
    val schedules: List<ScheduleRaw> = emptyList(),
)

@Serializable
private data class ScheduleRaw(
    val id: Long,
    val npcId: Int,
    val name: String = "",
    val startTime: Int = 0,
    val startTimeText: String = "",
    val startPoint: ScenePointRaw = ScenePointRaw(),
    val endPoint: ScenePointRaw = ScenePointRaw(),
    val week: List<Int> = emptyList(),
    val season: List<Int> = emptyList(),
    val weather: List<Int> = emptyList(),
    val isAstar: Int = 0,
    val relation: List<Int> = emptyList(),
)

@Serializable
private data class ScenePointRaw(
    val sceneId: Int = 0,
    val row: Int = 0,
    val col: Int = 0,
    val sceneName: String = "",
)
