# 恢复实现规格（RESUME_SPEC）

> 用途：工程师在限流恢复（2026-08-28 20:09:19 UTC+8）后，按本规格一次性补齐 UI 层并编译通过、提交。
> 背景：数据层 / 切片层 / 富文本层 / 导航脚手架已落地（未提交），但 **UI 页面（7 个 screen）+ `MainActivity.kt` 缺失**，且存在 2 处需修正的 bug。
> 范围：**仅前 3 个板块**（物品大全、配方查询、NPC 资料），其余（装饰预览 / 装备头饰拆分 / 工坊合集）为未来愿景，本期不做。

---

## 0. 当前已存在（勿重写，复用）

- 数据模型：`data/model/{ItemInfo,RecipeInfo,NpcInfo,NpcSchedule,RecipeMaterial,RichText,SpriteAtlas,SpriteAtlasFrame,ItemGroup,ScenePoint}.kt`
- 仓储：`data/repository/{DataRepository,SpriteRepository,SpriteCacheManager}.kt`
- 资源：`data/AssetManager.kt`、`data/source/{AssetLoader,CacheProvider,SpriteSlicer}.kt`
- 切片：`sprite/PlistParser.kt`、`data/source/SpriteSlicer.kt`(接口)、`platform/AndroidSpriteSlicer.kt`(实现)、`platform/AndroidSpriteCacheManager.kt`、`platform/AndroidCacheProvider.kt`
- 组件：`ui/components/{SpriteImage,MenuCard,RichText,CollapsibleTopBar}.kt`
- 导航：`ui/nav/{Navigator,Route,AppNavHost}.kt`、`ui/theme/Theme.kt`
- 资源文件：`shared/src/commonMain/composeResources/files/` 下全部 json/plist/png 与 `npcs/`

> 关键约束（用户明确要求）：**复制 miuix 的 example demo 后原地修改**，不是从零重写。
> 严格遵循 miuix 设计语言（见 `ARCHITECTURE.md` §1.2 / Q-F：用 `SmallTitle` 作块标题 + `BasicComponent(title/summary)` 作两行；菜单卡 = `Card { BasicComponent(startAction = Image) }`）。先读 `miuix/example/android` 与 `miuix/example/shared` 的对应页面再写。

---

## 1. 必须修正的 2 个 Bug

### 1.1 `loveItemId` 语义纠正（用户重点纠正）
文件：`data/model/NpcInfo.kt`

- **错误**：当前注释写 `@param loveItemId 喜好物品 id`。
- **正确语义**：`loveItemId` 是「**该 NPC 自己的好感度所对应的 items id**」——
  好感度本身在游戏数据里是一个 items 的 id；你给花菇送礼，她**好感度**增加，这个被增加的对象就是这个 items id。
  **它不等于「该 NPC 喜欢的物品 id」**（喜欢/讨厌/最爱分别由 `likeItems`/`hateItems`/`bestFavorItems` 表示）。
- **改动**：
  1. 修正 `NpcInfo.loveItemId` 的 KDoc 注释为上述正确含义。
  2. 在 NPC 详情页中，将 `loveItemId` 作为「好感度」图标/标签展示（用 `SpriteImage(loveItemId)` 取其切片图，文案写「好感度」），与 `likeItems`/`bestFavorItems`/`hateItems` 分区明确区分。

### 1.2 切片器 rotated 帧尺寸错误 + 偏移合成缺失
参考：`D:\1Project\nainiuzhen-wiki\smart_picture_tool.py` 的 `_slice_plist_new`（**新 plist 格式 = 我们的格式**，见下）。

**Bug A（尺寸错）**— `platform/AndroidSpriteSlicer.kt` 的 `slice()`：
- 当前：`if (frame.rotated) Bitmap.createBitmap(sheet, left, top, w, h)` —— **未交换 w/h**。
- 正确（对齐 Python）：rotated 帧在图集里占位为 `h × w`，裁剪框应为 `(left, top, left+h, top+w)`，即 `Bitmap.createBitmap(sheet, left, top, h, w)`。
- 旋转方向：`smart_picture_tool.py` 用 `Image.ROTATE_90`（PIL 顺时针 90）还原。Kotlin 侧用 `Matrix.postRotate(90)`（顺时针）与之对应；当前 `rotateCounterClockwise` 用 `-90` 在裁剪框已交换后等价于还原，但务必**对照图集里一个已知 rotated 图标做肉眼校验**，若上下颠倒/左右翻转则调换旋转方向。

**Bug B（偏移合成缺失）**— `sprite/PlistParser.kt` + `AndroidSpriteSlicer.slice()`：
- Python 在裁剪+旋转后，会把结果贴到一张 `sourceSize` 透明画布上，偏移为 `sourceColorRect`（`{{x,y},{w,h}}` 的 x,y）。
- 当前 Kotlin：`PlistParser` 读的是 `<key>offset</key>`（旧格式字段）而非 `<key>sourceColorRect</key>`；`slice()` 完全没用 `offset`/`sourceSize`，直接返回裁剪图。
- 改动：
  1. `PlistParser` 增加解析 `<key>sourceColorRect</key>\s*<string>\{\{(\d+),(\d+)\},\{(\d+),(\d+)\}\}</string>`，存入 `SpriteAtlasFrame.sourceColorRect: IntOffset`（保留 `offset`/`sourceSize` 字段不动，避免破坏其它调用）。
  2. `AndroidSpriteSlicer.slice()`：旋转/裁剪得到 `bitmap` 后，创建 `sourceSize` 透明 `Canvas`，在 `(sourceColorRect.x, sourceColorRect.y)` 处 `drawBitmap`。若 `sourceColorRect` 为 0,0 则该步为无操作（视觉不变）。
  3. **先确认我们的 plist 用的是 `sourceColorRect` 还是 `offset`**（grep 一个 .plist 文件即可），按实际字段对齐；两种都解析、以实际存在的为准。

> 验证：挑 3~5 个已知 rotated 的物品图标 + 1~2 个 NPC 立绘，在真机/模拟器上肉眼比对是否正确（无错位、无透明边、无翻转）。

---

## 2. 必须删除 / 解决的重复定义

- `ui/Locals.kt` 与 `ui/nav/Navigator.kt` **重复定义** `LocalDataRepository` / `LocalSpriteRepository`。
  - `SpriteImage.kt` 已 `import ...ui.nav.LocalSpriteRepository`，故**以 `Navigator.kt` 的 `staticCompositionLocalOf` 版本为准**。
  - **删除 `ui/Locals.kt`**（含 `compositionLocalOf` 版本），消除二义性。

---

## 3. 需新建的 UI 文件（清单）

> 全部放在 `shared/src/commonMain/kotlin/com/nainiuzhen/wiki/ui/` 下对应包。
> 复用 `MenuCard` / `SpriteImage` / `RichText` / `CollapsibleTopBar` 组件与 `LocalNavigator` / `LocalDataRepository` / `LocalSpriteRepository`。

1. **`home/HomeScreen.kt`** — 启动落地页：标题 + 3 个板块入口（`MenuCard` 跳 `ItemList`/`RecipeList`/`NpcList`）+ 设置入口（`Settings`）。
2. **`items/ItemListScreen.kt`** — 物品大全列表：
   - 顶部吸顶统计条「共 N 个物品」+ 多选筛选 chip（按 `categoryLabel`）。
   - `LazyColumn` 展示 `LocalDataRepository.current.itemsByCategory(selected)`，每项 `MenuCard`：左 `SpriteImage(iconFrameKey)` + 名称 + 分类；点击进详情。
3. **`items/ItemDetailScreen.kt`** — 物品详情：图标 + 名称 + 价格区（有 `price` 才显示）+ 来源 + `RichText(descRaw)`（最小高度=平均行高、最多 8 行展开）。
4. **`recipe/RecipeListScreen.kt`** — 配方查询列表：按 `typeLabel` 分组/多选；每项 `MenuCard`：产物 `SpriteImage(iconFrameKey)` + 名称 + 类型标签。
5. **`recipe/RecipeDetailScreen.kt`** — 配方详情：产物图标 + 原料列表（每个原料 `SpriteImage(material.id)` + 名称 + `×num`，名称用 `itemById` 查）；解锁条件 `deblockingDesc`；`RichText(descRaw)`。
6. **`npc/NpcListScreen.kt`** — NPC 资料列表：`MenuCard`：`NpcPortraitImage(id)` + 名称 + 住址/生日；点击进详情。
7. **`npc/NpcDetailScreen.kt`** — NPC 详情：
   - 立绘 `NpcPortraitImage` + 名称 + 简介 `RichText(descRaw)`。
   - **好感度**：`SpriteImage(loveItemId)` + 文案「好感度」（语义见 §1.1，区别于喜欢列表）。
   - **最爱 / 喜欢 / 讨厌**：分别用 `bestFavorItems`/`likeItems`/`hateItems` 列表，每项 `SpriteImage(id)` + 名称（`itemById`）。
   - 若 `maxStar > 0`：显示「日程」入口跳 `NpcSchedule(npcId)`；否则禁用。
8. **`npc/NpcScheduleScreen.kt`** — 日程子页：用 `LocalDataRepository.current.npcSchedules(npcId)`，按 `week`/`season`/`weather`/`startTimeText` 展示 `NpcSchedule`（复用 `SmallTitle`/`BasicComponent`）。
9. **`settings/SettingsScreen.kt`** — 设置：切片缓存清理按钮（调 `SpriteCacheManager.clear()` / `AndroidSpriteCacheManager`），显示「版本号 + 缓存大小」；可含深色开关（可选）。
10. **`app/src/main/kotlin/.../MainActivity.kt`**（Android 入口，在 `:app` 模块）：
    - `setContent { AppTheme(isDark) { ProvideLocals { AppNavHost(backStack, navigator) } } }`。
    - 启动时 `lifecycleScope.launch(Dispatchers.IO) { AssetManager(AndroidSpriteSlicer(), AndroidSpriteCacheManager()).loadAll(BuildConfig.DEBUG) }`，用 `CompositionLocalProvider(LocalDataRepository provides data, LocalSpriteRepository provides sprite, LocalNavigator provides Navigator(backStack))` 下发。
    - `backStack` 初始为 `mutableStateListOf<NavKey>(Route.Home)`。

### 入口接线
- `Route.kt`：删除 `Decoration` 路由（本期不做），保留 `Home/ItemList/RecipeList/NpcList/Settings/NpcSchedule`。
- `AppNavHost.kt`：删除 `entry<Route.Decoration>` 及其 import；其余 entry 指向上述新建 screen。

---

## 4. 编译与验证（提交前必须）

1. `./gradlew :app:assembleDebug`（或 `tools/build.ps1`，注意 `JAVA_HOME`/`ANDROID_HOME` 指向 `D:\Android` 的工具链）—— **必须编译通过**。
2. 在模拟器/真机跑通：首页 → 3 个板块列表 → 详情 → NPC 日程；确认：
   - 物品/配方图标正确（rotated 帧无错位，见 §1.2）；
   - 富文本颜色/换行正常；
   - `loveItemId` 在 NPC 详情显示为「好感度」而非「喜欢物品」；
   - 设置里清缓存后图标仍能重新切片。
3. 自测边界：空搜索/无结果列表、无图标物品（占位图）、`maxStar=0` 的 NPC 日程按钮禁用。

## 5. 提交

- 全部改动一次性提交（含 §1 修正、§2 删除、§3 新建），commit message 形如 `feat(ui): 补齐物品/配方/NPC 三板块页面与 MainActivity，修正切片 rotated 与 loveItemId 语义`。
- 不单独提交，避免碎片化；提交后报告 commit hash。
