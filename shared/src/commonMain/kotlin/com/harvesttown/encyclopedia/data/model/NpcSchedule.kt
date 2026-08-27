package com.harvesttown.encyclopedia.data.model

/**
 * 日程场景点。
 *
 * @param sceneId 场景 id。
 * @param row 行。
 * @param col 列。
 * @param sceneName 场景名（用于箭头连接展示）。
 */
data class ScenePoint(
    val sceneId: Int,
    val row: Int,
    val col: Int,
    val sceneName: String,
)

/**
 * NPC 日程，由 `npc_ai_database.json` 的 `schedules[]` 解析而来。
 *
 * 筛选维度：
 * - [week]：星期 1..7。
 * - [season]：季节 1..4。
 * - [weather]：天气 1..5。
 * - [isAstar]：婚姻 0=未婚 / 1=已婚（需求指定以此为准）。
 *
 * @param id 日程 id。
 * @param npcId 所属 NPC id。
 * @param name 日程名（大字展示）。
 * @param startTime 开始时间（分钟，如 360 → 06:00）。
 * @param startTimeText 开始时间文本。
 * @param startPoint 起点场景。
 * @param endPoint 终点场景。
 * @param week 适用的星期列表。
 * @param season 适用的季节列表。
 * @param weather 适用的天气列表。
 * @param isAstar 婚姻状态。
 * @param relation 婚姻关联字段。
 */
data class NpcSchedule(
    val id: Long,
    val npcId: Int,
    val name: String,
    val startTime: Int,
    val startTimeText: String,
    val startPoint: ScenePoint,
    val endPoint: ScenePoint,
    val week: List<Int>,
    val season: List<Int>,
    val weather: List<Int>,
    val isAstar: Int,
    val relation: List<Int>,
)
