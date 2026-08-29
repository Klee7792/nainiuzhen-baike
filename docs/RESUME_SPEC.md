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

---

## 6. 附录：版本演进与当前实现状态（v1.0.10 / build-10）

> 本 RESUME_SPEC 为限流恢复初期（2026-08-28）的「补齐 UI」规格；以下记录相对本规格的实际落地偏差与 v8/v9 关键修复，便于后续接手工程师对齐最新状态。版本号约定：`vN ↔ 1.0.N ↔ build-N`，当前最新 **v1.0.10 / build-10**（常量 `APP_VERSION_NAME` 位于 `utils/AppState.kt`）。

### 6.1 与原规格的关键偏差（已实现）
1. **资源读取：从 `composeResources/files/` 改为 Android 原生 `assets/`**（build #2 修复）。
   - 原规格 §0 假设 `Res.readBytes("files/...")`。实测 CMP composeResources 资源**不会合并进纯 Android 的 `:app`** → 真机 `MissingResourceException` 闪退。
   - 现状：`AssetLoader` 经 `expect fun readAssetBytes(path)`，Android 侧 `PlatformAssetReader` 用 `AssetManager.open(path)`；121 个文件复制到 `app/src/main/assets/`。（`shared/.../composeResources/files/` 为冗余死数据。）
2. **切片加载改为异步（启动变慢排查 #1）**：`SpriteImage`/`NpcPortraitImage`/`StarImage` 原在 `remember` 中**同步**取图（主线程切片）；现改为 `produceState` + `withContext(Dispatchers.IO)` 异步取图，先占位后替换。
3. **底栏方案**：`floatingNavigationBarStyle` 0=Miuix / 1=iOS。iOS 风格移植自 miuix demo `LiquidGlassNavigationBar`（液态玻璃：折射 / 高光 / 按住拖动切换 / 选中果冻弹跳 / 多一圈层级），位于 `ui/components/liquid/`（package 改为 `com.nainiuzhen.wiki.ui.components.liquid`）。角标默认关闭、启用时红色且置于 icon 右上角外侧。
4. **富文本解析修正（build #9）**：繁荣度「不带字号」格式 `/#颜色#内容/#` 修复正确区分。语法：`/#RRGGBB#内容/#`（无字号）与 `/#RRGGBB#SIZE#内容/#`（SIZE=sp）。
5. **关于页闪退修复（build #10）**：去除 `AboutScreen` 内层 `BgEffectBackground` 对同一个 backdrop 的重复 `layerBackdrop` 录制（重入异常）；仅保留外层 `Box` 的 `layerBackdrop`。需真机验证。
6. **NPC dialog**：顶栏立绘 120%（144.dp）；好感 max 改为 `[★] N 心 [★]`（`MiuixIcons.FavoritesFill`）；最爱/喜欢/讨厌标题加同色下划线；弹窗内距最小化；按钮对称（`fillMaxWidth(0.49f)`）。
7. **日程筛选区模糊同步顶栏**：筛选区由纯 `surface` 改为采样同一 backdrop 的 `textureBlur`。
8. **物品/配方计数左对齐**：`TextAlign.Start` + 与搜索框/网格同 12.dp 左对齐；收起（无搜索词）时无额外间距。

### 6.2 工程 / 工具链现状
- **Gradle**：AGP 9.3.2 要求 **Gradle ≥ 9.5**，工程用 **Gradle 9.6.1**（原规格 §4 提到的 8.9 已不可用）。miuix 复合构建根 `D:/1Project/nainiuzhen-wiki/miuix`。
- **缓存版本标记**：`version.txt`（整数，由 `AndroidSpriteCacheManager.currentVersion()` 提供），非 `BuildConfig.VERSION_CODE`。
- **出包**：`tools/build.ps1` 递增 `builds/build_number.txt` → 双包 → git commit + `build-N` tag。
- **入口**：`MainActivity` 注入切片器/缓存器 → `App(...)`；`App.kt` 的 `AppRoot` 在 `LaunchedEffect` 中以 `Dispatchers.IO` 执行 `loadAll`，进程存活复用 `cachedLoadedData`。

### 6.3 待确认
- 关于页闪退修复靠代码审查定位，需真机验证（必要时抓 logcat）。
- 日程筛选「从婚姻筛选下开始」的渐进模糊细节需结合真机截图最终确认。
- 实际文件命名已演进（见 `ARCHITECTURE.md` §13.3），以工程实际文件为准。

