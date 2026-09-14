# 奶牛镇百科 · 大屏与横屏适配方案

| 项 | 值 |
| --- | --- |
| 文档状态 | **已定稿基线**（2026-09-13 主理人拍板，见第 13 节） |
| 适用版本 | v28 (versionCode 28 / 1.2.8) |
| 编制日期 | 2026-09-13 |
| 本轮范围 | **仅方案，不改任何代码** |
| 目标工程 | `nainiuzhen-baike`（CMP + miuix 0.9.4，compileSdk 37 / targetSdk 37 / minSdk 24） |

---

## 1. 背景与目标

### 1.1 用户提出的问题

| # | 问题 | 现状 |
| --- | --- | --- |
| Q1 | 大屏**竖屏**固定 6 列，卡片等比放大成"大号手机" | 确认 |
| Q2 | 大屏**横屏**同样固定 6 列 | 确认 |
| Q3 | 物品详情横屏信息还行、**按钮还在** | 确认 |
| Q4 | 配方 / NPC 详情横屏**按钮区整块消失**，且 dialog 底与屏幕底之间还空着约一个 dialog 的高度 | 确认，**P0 阻塞** |
| Q5 | NPC **日程无入口**（因为详情按钮点不到） | 确认，**P0 阻塞** |
| Q6 | 屏幕形态越来越多（华为阔折叠、阔直板），怎么判断手机 / 平板 | 见第 4 节 |
| Q7 | 常规手机要不要关掉横屏 | **已定**：设置页加「手机横屏」开关（默认关）；**系统导航栏旋转按钮不可用**，见第 8 节 |

### 1.2 目标

建立一套**以「窗口可用空间」为唯一依据**的布局系统，使得：

1. 卡片尺寸始终落在设定的 **[min, max]** 区间内，不同屏幕**自动分 n 列**，不再等比放大；
2. 详情容器在**任意可用高度**下都保证「底部按钮永远可见」；
3. 大屏横屏有**专门的形态**（列表-详情双栏），而不是把手机竖屏布局拉宽；
4. 判定逻辑**不依赖物理设备型号**，能覆盖折叠屏、阔比例屏、分屏、自由窗口。

### 1.3 非目标

- 不改变业务数据、路由语义、黑名单等既有逻辑；
- 不引入需要服务端配合的方案；
- 本轮不做平板专属的"专业版"布局（如三栏）。

---

## 2. 现状与问题清单（含代码证据）

> 全部证据由代码调研逐行核实，路径相对 `nainiuzhen-baike/`。

### 2.1 问题一览

| 编号 | 级别 | 问题 | 证据（文件:行） |
| --- | --- | --- | --- |
| **P0-1** | 阻塞 | 详情弹窗**按钮区无高度保护**，横屏矮屏被挤出屏幕 | `shared/.../ui/components/BasicDetailDialog.kt:46-51`（可滚动内容列）排在 `:55-63`（按钮 Row）之前；按钮 Row 无 `weight` / 无最小高度 |
| **P0-2** | 阻塞 | NPC 弹窗三重限高叠加，横屏必然压爆 → **日程入口不可达** | `shared/.../ui/npc/NpcDetailScreen.kt:121-126`（整体 `heightIn(max=640.dp)`）+ `:167`（header 硬编码 `144.dp`）+ `:134-137`（中部 `heightIn(max=400.dp)`） |
| **P0-3** | 阻塞 | miuix 把手机横屏判为「小屏」，走贴底定位**且不享受** 2/3 限高 | miuix `layout/DialogContentLayout.kt:422-427` `isLargeScreen() = 高≥480dp && 宽≥840dp`；`:295-296` 小屏 `BottomCenter`；`:351` 仅大屏 `heightIn(max = windowHeight*2/3)` |
| **P1-1** | 高 | 物品列表列数硬编码 6 | `ui/items/ItemListScreen.kt:126` `GridCells.Fixed(6)` |
| **P1-2** | 高 | 配方列表列数硬编码 6 | `ui/recipe/RecipeListScreen.kt:128` `GridCells.Fixed(6)`（`:16` 的 `GridItemSpan` 是**死导入**，全文件未使用） |
| **P1-3** | 高 | NPC 列表列数硬编码 3 | `ui/npc/NpcListScreen.kt:121` `GridCells.Fixed(3)` |
| **P1-4** | 高 | 全工程**零响应式代码** | 对 `LocalConfiguration / orientation / LANDSCAPE / screenWidthDp / screenHeightDp / WindowSizeClass / isLandscape / BoxWithConstraints / LocalWindowInfo` 全量搜索 = **0 命中** |
| **P2-1** | 中 | 详情内容文件**自身不含滚动**，滚动能力全在容器里 | `RecipeDetailScreen.kt` 全文无 `verticalScroll`/`heightIn`；`NpcDetailScreen.kt` body(`244-270`) 无滚动，靠 `:135` 外层提供 |
| **P2-2** | 中 | 弹窗限高值是**魔法数字**，与实际可用高度脱钩 | `BasicDetailDialog.kt:37` `maxHeight: Dp = 640.dp`；`NpcDetailScreen.kt:121` 640dp / `:134` 400dp |
| **P2-3** | 中 | 其它弹窗同样硬编码限高 | `FilterPopup.kt:50` 600dp；`FilterChipDialog.kt:53/87` 600dp；`RecipeFilterDialog.kt:49/59` 480dp |

### 2.2 P0-1 的失效机制（为什么按钮会消失）

`Column` 的测量顺序是从上到下**依次分配剩余高度**：

```
Column (fillMaxWidth)
├── 内容列  ← .heightIn(max = 640.dp).verticalScroll(...)
│              带 verticalScroll 的节点会「吃掉」父级给的约束高度
└── 按钮 Row ← 无 weight、无 minHeight → 拿到剩余高度 ≈ 0
```

在竖屏（可用高 > 640dp）时，内容列被卡在 640dp，按钮行还有空间 → 正常。
在**横屏**（手机横屏可用高通常 **360–420dp** < 640dp）时：
1. 内容列吃掉**全部**可用高度；
2. 按钮 Row 分到的高度趋近 0 → **消失**；
3. 物品详情内容较短、`verticalScroll` 不占满 → 按钮**侥幸可见**（这解释了 Q3 与 Q4 的差异）。

**结论：主因是我们自己的布局缺陷（内容列与按钮行缺权重约束），miuix 的小屏判定只是加重了症状。** 用户猜测的"miuix bug"需要修正为：miuix 的 `isLargeScreen()` 阈值（`480×840`）与工程既有的固定限高**叠加**，共同导致横屏空间被吃干。

### 2.3 关于"dialog 底部与屏幕底还空着一个 dialog 高度"

**待真机确认**。当前推断：miuix 在「小屏」分支用 `Alignment.BottomCenter` 定位（`DialogContentLayout.kt:295-296`），而我们的 `Column` 带 `heightIn(max = 640.dp)`；当内容实际高度**小于** 640dp 时，容器仍按较大的测量值占位，于是底部出现空档。这条**不作为独立缺陷立项**——第 7 节的三分区重构（改用 `rememberDialogMaxHeight` 限高 + `weight(1f, fill=false)`）会一并消除，届时以真机复核为准。

### 2.4 现状配置（决定了方案边界）

| 配置 | 当前值 | 文件 |
| --- | --- | --- |
| `screenOrientation` | **未声明**（跟随系统，可自由旋转） | `app/src/main/AndroidManifest.xml` |
| `configChanges` | 已含 `orientation\|screenSize\|screenLayout\|smallestScreenSize` → 旋转**不重建 Activity** | 同上 |
| `resizeableActivity` | 未声明 → API 24+ 默认 `true` | 同上 |
| `compileSdk` / `targetSdk` / `minSdk` | **37 / 37 / 24** | `app/build.gradle.kts:10,18,19` |
| miuix | 0.9.4，composite build（`includeBuild("D:/1Project/nainiuzhen-wiki/miuix")`） | `shared/build.gradle.kts:25-30`、`settings.gradle.kts:36` |

**两条对方案有决定性影响的事实：**

- ✅ `configChanges` 已覆盖旋转 → 布局切换成本低，不会有重建闪烁，**但也不会有 `onCreate` 重新读资源**，所以必须用**运行期**判定（`WindowSizeClass`），不能用 `layout-land` 资源。
- ⚠️ `targetSdk = 37` → 见 3.2。

---

## 3. 关键事实（业界调研结论）

### 3.1 官方尺寸分级 API：已换代

**结论：用 `WindowSizeClass`（Jetpack WindowManager），不要再引入 `material3-window-size-class`。**

| 项 | 内容 |
| --- | --- |
| 推荐类 | `androidx.window.core.layout.WindowSizeClass` |
| 宽度断点 | Compact `<600` ｜ Medium `600–839` ｜ Expanded `840–1199` ｜ **Large `1200–1599`** ｜ **Extra-large `≥1600`** |
| 高度断点 | Compact `<480` ｜ Medium `480–900` ｜ Expanded `≥900` |
| 判定方法 | `isWidthAtLeastBreakpoint(600/840/1200/1600)`、`isAtLeastBreakpoint(w,h)`；**必须从大到小判断** |
| 已废弃 | 旧枚举 `WindowWidthSizeClass` / `WindowHeightSizeClass` 官方明确"不再发展，请改用 `WindowSizeClass`" |
| 统一入口 | M3 Adaptive 的 `currentWindowAdaptiveInfo().windowSizeClass` |

来源：<https://m3.material.io/foundations/layout/breakpoints>、<https://developer.android.com/reference/kotlin/androidx/window/core/layout/WindowSizeClass>、<https://developer.android.com/develop/ui/views/layout/use-window-size-classes>

> ⚠️ **一条未证实项**：`material3-window-size-class` 这个 artifact 是否被官方**明确标记废弃**，调研**未查到官方原文**（文档页抓取失败）。可确认的是 Google 全部新文档/示例已不再使用它。**方案按"不再使用该 artifact"执行，但行文中不声称"官方已废弃"。**

### 3.2 ⚠️ 最重要的一条：`targetSdk 37` 锁死了大屏方向

| Android 版本 | 行为 |
| --- | --- |
| **Android 16 (API 36)** | targetSdk ≥36 的应用，在 **smallest width ≥ 600dp** 的显示上，`screenOrientation`、`setRequestedOrientation()`、`resizeableActivity`、`minAspectRatio`、`maxAspectRatio` **全部被忽略**；可用 `PROPERTY_COMPAT_ALLOW_RESTRICTED_RESIZABILITY` **临时** opt-out |
| **Android 17 (API 37)** | **取消 opt-out**，≥sw600dp **永远**忽略上述限制 |
| **例外** | `< sw600dp`（绝大多数手机、竖折外屏）；`appCategory=game`；用户在系统宽高比设置里显式 opt-in |

来源：<https://developer.android.com/about/versions/16/behavior-changes-16>、<https://developer.android.com/about/versions/17/changes/ff-restrictions-ignored>

**对本工程的推论：**

1. 本工程 `targetSdk = 37`，**大屏（≥sw600dp）上任何"锁方向"的写法都无效** → 大屏横屏布局是**必须做**的，不是可选项。
2. **手机（<sw600dp）** 仍受例外保护 → 手机锁竖屏**技术上仍然有效**（但见 3.4 的 ROM 变数）。
3. 大屏还会忽略 `maxAspectRatio` → 布局必须**自身防拉伸**（不能靠系统信箱化兜底）。

### 3.3 折叠屏 / 阔比例屏

**判定原则（官方）**：不要用物理设备属性判断布局，官方明确 "Avoid using physical hardware values for making layout decisions"。应以 **当前窗口 metrics** 为准。

**`FoldingFeature` 能拿到的信息**（`WindowInfoTracker`）：

| 属性 | 取值 |
| --- | --- |
| `state` | `FLAT` / `HALF_OPENED` |
| `orientation` | `HORIZONTAL` / `VERTICAL` |
| `occlusionType` | `NONE` / `FULL` |
| `isSeparating` | 跨屏时为 `true` |
| `bounds` | 铰链/折痕位置 |

桌面姿态 = `HALF_OPENED` + `HORIZONTAL`；书本姿态 = `HALF_OPENED` + `VERTICAL`。

**用法纪律**：`FoldingFeature` **只用于铰链避让 / 姿态特化**，**不要**用它区分"手机还是平板"——后者一律用窗口宽度档。

**华为阔折叠实测规格**（华为开发者文档）：

| 机型 | 形态 | 比例 | vp 宽×高 |
| --- | --- | --- | --- |
| Pura X | 折叠（横向） | 1:1 | **326×326** |
| Pura X | 展开（竖向） | 16:10 | **440×707** |
| Pura X Max | 展开（横向） | 10:14 | **940×665** |
| Pura X View | — | 16:9.5 | 440×744 |

来源：<https://developer.huawei.com/consumer/cn/doc/doccenter-multi-device/bpta-purax-guide>

**关键试探**：Pura X 内屏宽度 **440vp < 600** → 只按宽度会被判为 **Compact（手机档）**。但该屏 440×707 实际上接近"大号手机竖屏"，**单栏是合理的**；真正的风险在 **Pura X Max 展开 940×665**：宽度 940 → Expanded（该双栏），高度仅 665 → Medium 高度（**横向偏矮**）。→ **双栏方案必须按"矮屏"设计，不能假设高度宽裕。**

### 3.4 国内 ROM 与竖屏锁定

- 【官方事实】Android 12+ 起，**设备制造商可把个别屏幕配置为忽略 `screenOrientation`**（针对平板尺寸的折叠内屏等）。
- 【社区经验】国内 ROM 普遍提供**单应用方向**设置（如澎湃OS：应用管理 → 横屏模式：跟随系统/强制横屏/强制竖屏），即厂商与用户都能覆盖应用声明。
- 【未查到】国内 ROM 在**手机上**忽略 `portrait` 锁定的官方文档级证据 —— **未查到**，仅有官方"制造商可忽略"（针对大屏）与社区证据。
- 【官方建议】`configChanges` 只能减少重建，**状态保存应走 ViewModel / SavedStateHandle**。

来源：<https://developer.android.com/reference/android/R.attr#screenOrientation>

### 3.5 网格自适应：`Adaptive` 不够用

【官方事实】`GridCells.Adaptive(minSize)` 的构造函数**只有 `minSize`** —— **没有最大尺寸、没有最大列数上限**。窗口变宽时列数与单卡宽度会**无限增长**。`GridCells` 是接口，**可以自定义实现**。

来源：<https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/grid/GridCells.Adaptive>

**官方设计规范参考**（Material 响应式栅格）：手机 4 栏 / 600–904dp 为 8 栏 / ≥905dp 为 12 栏；**布局超过 1600dp 应限制最大宽度并居中**；内容正文最大宽度建议 ≤1040–1200dp。

来源：<https://m3.material.io/foundations/layout/breakpoints>

### 3.6 列表-详情双栏：**CMP 可用**（这是一个常被误传的点）

【官方事实】Compose Multiplatform **1.7.0 起**，JetBrains 已把 `material3.adaptive` 的 `adaptive` / `adaptive-layout` / `adaptive-navigation` 三个模块移植进**通用代码**。网上"`ListDetailPaneScaffold` 仅限 Android"的说法是 **1.7 之前**的情况，**现已不成立**。

| 项 | 内容 |
| --- | --- |
| Android 坐标 | `androidx.compose.material3.adaptive:adaptive*`，**1.1.0 稳定**（2025-03-12） |
| CMP 通用坐标 | `org.jetbrains.compose.material3.adaptive:adaptive*`（CMP 1.9.x 对应 `1.2.0-alpha05`） |
| 底层窗口 | `org.jetbrains.androidx.window:window-core` |
| 主要 API | `ListDetailPaneScaffold`（低层）/ **`NavigableListDetailPaneScaffold`（推荐，含导航 + 预测性返回）** / `SupportingPaneScaffold` / `rememberListDetailPaneScaffoldNavigator()` / `AnimatedPane` |
| 标注 | `@ExperimentalMaterial3AdaptiveApi` |
| ⚠️ 坑 | CMP 下**不要**调 `calculateWindowSizeClass()`（1.7.x 明确不可用于 common），一律用 **`currentWindowAdaptiveInfo()`** |

来源：<https://www.jetbrains.com/help/kotlin-multiplatform-dev/whats-new-compose-170.html>、<https://developer.android.com/develop/ui/compose/layouts/adaptive/list-detail>

【官方推荐】canonical layouts 明确：**expanded 宽度用 list + detail 双栏；compact/medium 只显示单栏**。即"大屏点列表 → 右栏详情"是官方**首选**形态。

【替代方案（不用 pane 库时）】`Row { 左 weight + VerticalDivider + 右 weight }` + `AnimatedContent` + 状态提升 + `BackHandler`。

### 3.7 依赖现状：**零新增依赖即可起步**

工程 build 脚本目前**没有**声明 `androidx.window` / `material3-adaptive` / `material3-window-size-class`，但构建产物显示它们**已作为传递依赖进入 classpath**：

```
androidx.window:window:1.5.0
androidx.window:window-core-android:1.5.0
org.jetbrains.compose.material3:material3-window-size-class-android:1.12.0-alpha03
androidx.compose.material3:material3-window-size-class-android:1.5.0-alpha22
```

（证据：`app/build/intermediates/.../release-artifact-libraries.xml`）

→ **窗口判定（`androidx.window` 的 `WindowSizeClass`）可以立刻使用**。但传依赖版本不可控，**正式落地时应显式声明**以保证 API 可见性与版本稳定。

---

## 4. 屏幕判定策略（方案基础层）

### 4.1 核心原则

> **不判断"设备是什么"，只判断"当前窗口有多少空间"。**

理由：分屏、自由窗口、折叠展开态下，同一台设备的"手机/平板"属性会变；而 `sw600dp` 资源限定符取的是**显示区最小边**，**不随窗口变化**，在分屏/自由窗口下会给出错误答案。

| 手段 | 用途 | 禁止用途 |
| --- | --- | --- |
| `LocalWindowInfo.current.containerDpSize`（宽/高）+ 自算断点 | ✅ 所有布局分支（已实现，`WindowClass.kt`，零新增依赖） | — |
| `sw600dp` 资源限定符 | ✅ 整机级静态资源（dimens / 图片） | ❌ 布局分支 |
| `WindowInfoTracker` + `FoldingFeature` | ✅ 铰链避让、Tabletop/Book 姿态特化 | ❌ 判断手机/平板 |

> ⚠️ 方案稿原计划用 `currentWindowAdaptiveInfo().windowSizeClass`，**已否决**（理由见 §4.2.1）。现统一以 `LocalWindowInfo.current.containerDpSize` 为唯一依据。

### 4.2 判定函数（已落地 · `WindowClass.kt`）

> ⚠️ **与早期规划不一致**：本文档早期（方案稿）落点是 `currentWindowAdaptiveInfo().windowSizeClass`，**实际未采用**。已实现版本改用 `LocalWindowInfo.current.containerDpSize`，判点与 AndroidX `WindowSizeClass` 官方断点值一致、零新增依赖。

```kotlin
// 落点：shared/src/commonMain/kotlin/com/nainiuzhen/wiki/ui/adaptive/WindowClass.kt（已提交 1f98c6f）
enum class WindowClass { COMPACT, MEDIUM, EXPANDED, LARGE }   // 横向分档

@Composable
fun rememberWindowClass(): WindowClass {
    val width = LocalWindowInfo.current.containerDpSize.width
    return when {
        width >= 1200.dp -> WindowClass.LARGE      // >=1200
        width >= 840.dp  -> WindowClass.EXPANDED   // >=840
        width >= 600.dp  -> WindowClass.MEDIUM     // >=600
        else             -> WindowClass.COMPACT    // <600
    }
}

/** 矮屏判定：横屏手机（如 800x360）命中；Pura X Max 展开态(940x665) 高 665dp > 480dp，不命中 */
@Composable
fun isShortWindow(): Boolean =
    LocalWindowInfo.current.containerDpSize.height < 480.dp
```

> **注意**：**从大到小判断**（小断点会匹配所有更大档），否则全部落到 COMPACT。

#### 4.2.1 为什么不用 `currentWindowAdaptiveInfo()`（已定决策，保留备查）

1. **本地 Gradle 缓存缺 `material3-adaptive`**：`currentWindowAdaptiveInfo()` 来自 `org.jetbrains.compose.material3.adaptive:*`，本机缓存里**没有**该 artifact，引入需**联网拉依赖**，与本轮「零新增依赖」约束冲突；
2. **metadata 编译风险**：`:shared` 目前只有**单一 `androidLibrary` target**，在 `commonMain` 直接引用 Android-only 的 AAR（`androidx.window` 体系）存在 Kotlin Multiplatform metadata 编译兼容风险。

→ 断点比较本身是稳定常数，用 Compose 自带的 `LocalWindowInfo.current.containerDpSize` 语义等价、零新增依赖；后续若需折叠铰链避让等高级形态，再评估是否显式引入 `androidx.window`。

### 4.3 三档形态 → 布局决策表（建议）

| 窗口宽 | 形态名 | 列表网格 | 详情形态 | 说明 |
| --- | --- | --- | --- | --- |
| `<600` | **COMPACT**（手机竖屏 / Pura X 外屏 326×326 / 竖屏折叠） | 按屏宽算列（约 4–6 列） | 弹窗（三分区重构） | 即当前手机体验，仅修 P0 |
| `600–839` | **MEDIUM**（大手机横屏 / 竖屏平板） | 列数增加（约 8–10 列） | 弹窗（三分区） | 二期再评估是否提前上双栏 |
| `≥840` | **EXPANDED / LARGE**（平板横屏 / Pura X Max 展开 940×665 / 桌面窗口） | 列数无限、填满整行（不封顶不限宽，见 §5.5） | **一期弹窗（三分区）→ 二期列表-详情双栏** | 官方推荐形态；**分两期落地** |
| 任意宽 + **高<480** | **矮屏叠加态** | 列数不变 | 强制三分区；双栏时压缩 header | 横屏手机、阔比例屏 |

---

## 5. 方案 A：自适应网格（解决 Q1 / Q2）

### 5.1 为什么不能直接用 `GridCells.Adaptive`

`Adaptive(minSize)` **只有下限**：大屏上列数会一直涨、单卡会一直宽，正是用户抱怨的"等比放大"的反面——变成"无限增殖"。需要**上下限双约束**。

### 5.2 早期草案（⚠️ 已被 §5.5 / `AdaptiveGrid.kt` 取代，保留备查，勿照此实现）

> **本节为方案早期草案，与最终实现不一致**：草案里带了 `maxColumns` 列数上限 + `maxCard` 内容区限宽居中；**实际落地（`AdaptiveGrid.kt`，commit `fc4d10a`）不设列数上限、不设内容区限宽**，网格永远填满整行。以 §5.5 与下方最终函数为准。

```kotlin
// ❌ 早期草案（未采用）：带 maxColumns / maxCard 限宽
fun AdaptiveIconGrid(hPadding, spacing, minCard, maxCard, maxColumns, content) { ... }  // 含 coerceIn(1,maxColumns) 与 gridWidth 限宽居中

// ✅ 最终实现（AdaptiveGrid.kt）：只按 minCard 求最大列数，无上限、无限宽
fun adaptiveGridColumns(availableWidth: Dp, spacing: Dp, minCard: Dp): Int =
    ((availableWidth + spacing) / (minCard + spacing)).toInt().coerceAtLeast(1)

@Composable
fun AdaptiveIconGrid(
    hPadding: Dp, spacing: Dp, minCard: Dp,
    modifier: Modifier = Modifier,
    grid: @Composable (columns: Int) -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        grid(adaptiveGridColumns(maxWidth - hPadding * 2, spacing, minCard))
    }
}
// 卡片内部一律 Modifier.fillMaxWidth() —— 宽度由列数决定，不需要在卡片里写死尺寸
```

**设计要点（与 §5.5 一致）**

1. **列数只取下限 1**：`coerceAtLeast(1)` 防小屏出 0 列；**不设上限**（`maxColumns` 已删除）。
2. **不设内容区限宽、不居中**：`GridCells.Fixed(columns)` 均分填满整行，**永不出两侧留白**（代价见 §5.5：超宽屏列数多）。
3. **卡片自身不设尺寸**：只 `fillMaxWidth()`，宽度完全由列数推导，只有一个真相源。
4. **卡片保持方形**：现有 `aspectRatio(1f)` 保持不变，卡片高度随宽度走。

### 5.3 基线实测：你现在看到的卡片到底多宽

从代码实测（`ItemListScreen.kt:124-140`、`RecipeListScreen.kt:126-142`、`NpcListScreen.kt:120-135`）：

| 列表 | 列数 | 横 padding | 横向间距 | 单卡宽（dp） | 单卡宽（px @K40） |
| --- | --- | --- | --- | --- | --- |
| 物品大全 | 6 | 12dp | **8dp** | **54.7dp** | **≈151px** |
| 配方查询 | 6 | 12dp | **8dp** | **54.7dp** | **≈151px** |
| NPC 资料 | 3 | 12dp | **12dp** | **114.7dp** | **≈316px** |

计算式（`GridCells.Fixed(n)` 均分，**必然填满整行、不会剩宽度**）：

- 物品 / 配方：`card = (屏宽 − 24 − 8×(n−1)) / n`
- NPC：`card = (屏宽 − 24 − 12×(n−1)) / n`

> ⚠️ **密度歧义（需你确认）**：项目记忆里同时存有 `swDp≈392 → 1dp=2.755px` 与 `swDp≈429 → 1dp=2.518px` 两组换算，对应**同一块 1080px 宽的屏**。两者 **px 结论一致**（≈151px / ≈316px），只有 dp 数字不同。下文一律按 **392dp / 1dp=2.755px** 写；若你机上实际是 429dp，dp 数字整体 ×1.12（px 不变）。

### 5.4 ✅ 卡片尺寸目标：±20px（= ±7.26dp）

20px ÷ 2.755 = **7.26dp**。以基线 54.7dp / 114.7dp 为中心，目标区间：

| 列表 | 目标区间（dp） | 目标区间（px @K40） |
| --- | --- | --- |
| 物品 / 配方 | **47.4 – 61.9dp** | 131 – 171px |
| NPC | **107.4 – 121.9dp** | 296 – 336px |

### 5.5 列数规则：**填满整行，不留两侧空白**

> 直接回答你的疑问 ——「固定卡片尺寸，会不会导致除了列间距、还有两侧间距比较宽？」**不会**。因为规则是"**先定列数，卡片再均分填满整行**"，而不是"先定卡片尺寸，再把剩余宽度变成留白"。

**规则：取「让单卡宽度 ≥ `minCard` 的最大列数」，格子均分填满整行。**

- `minCard` = 目标区间**下沿**：物品 / 配方 **47dp**，NPC **104dp**；
- **不设** `maxCard` 上限，**也不设列数上限、不设内容区限宽** → 网格**永远填满整行，永不出两侧留白**；
- 代价：超宽屏列数会很多（1200dp → 21 列）。**✅ 已确认接受**（每卡仍 ≈50dp，点击目标充足）。

**推算结果（全部填满整行、无两侧留白）**

物品 / 配方：

| 屏宽 | 列数 | 单卡宽 | 与基线（54.7dp）差 |
| --- | --- | --- | --- |
| 360dp（小屏手机） | 6 | 49.3dp | −5.3dp（−15px）✓ |
| **392dp（K40 竖屏）** | **6** | **54.7dp** | **0（= 现状）** |
| 411dp | 7 | 48.4dp | −6.3dp（−17px）✓ |
| 440dp（Pura X 内屏） | 7 | 52.6dp | −2.1dp（−6px）✓ |
| 600dp（平板竖屏） | 10 | 50.4dp | −4.3dp（−12px）✓ |
| 840dp（平板横屏） | 14 | 50.9dp | −3.8dp（−11px）✓ |
| 940dp（Pura X Max 展开） | 16 | 49.8dp | −4.9dp（−14px）✓ |
| 1200dp（桌面窗口） | 21 | 48.4dp | −6.3dp（−17px）✓ |

→ **卡宽全程落在 48–55dp，全部在 ±20px 以内**；大屏靠**加列**消化宽度，卡片**不放大**、也**无两侧留白**。

NPC 资料：

| 屏宽 | 列数 | 单卡宽 | 与基线（114.7dp）差 |
| --- | --- | --- | --- |
| 360dp | 3 | 104.0dp | −10.7dp（−29px）⚠️ |
| **392dp（K40 竖屏）** | **3** | **114.7dp** | **0（= 现状）** |
| 440dp | 3 | 130.7dp | +16.0dp（+44px）⚠️ |
| 480dp | 4 | 105.0dp | −9.7dp（−27px）⚠️ |
| 600dp | 5 | 105.6dp | −9.1dp（−25px）⚠️ |
| 840dp | 7 | 106.3dp | −8.4dp（−23px）⚠️ |
| 1200dp | 10 | 106.8dp | −7.9dp（−22px）⚠️ |

> ⚠️ **NPC 做不到 ±20px，这是数学限制，不是实现问题。** NPC 只有 **3 列**，列数每变一档单卡宽度就跳 ~25%（物品/配方有 6 列，粒度细，所以能压进 ±20px）。
>
> **但按你「保持 K40 实际宽度、高度，只做一点波动」的要求，实际表现是合格的**：手机段（360–470dp）NPC **稳定保持 3 列**，卡宽 **104–131dp** —— 相对 K40 基准 114.7dp 只波动 **−9% ~ +14%**（−29px ~ +44px）。到 480dp 才跳到 4 列（105dp），此后稳定在 105–107dp。上表 600dp 之后的 105–107dp 属于"跳档后重新稳定"，**不是持续漂移**。
>
> **高度自动跟随，不需要单独参数**：NPC 卡片是 `Card` → 立绘区 `Modifier.fillMaxWidth().aspectRatio(1f)`（`NpcListScreen.kt:173-184`）**正方形**，名称胶囊在下方。所以**控住宽度就等于控住高度**（卡总高 ≈ 卡宽 + 胶囊约 28dp）。

---

## 6. 方案 B：列表-详情双栏（大屏）

### 6.0 先说清楚：「一期弹窗 → 二期双栏」到底指什么

这两个词说的**只有一件事：大屏上点卡片之后，详情出现在哪儿**。列表本身、数据、路由全都不变。

| | **一期（先做）** | **二期（后做）** |
| --- | --- | --- |
| 大屏点卡片 | 详情以**弹窗**弹出，**盖住**列表 | 详情显示在**右侧面板**，列表**仍在左边** |
| 屏幕分成几栏 | 1 栏（弹窗外加一层浮层） | 2 栏（左列表 + 右详情） |
| 横向空间利用 | 差 —— 弹窗宽度有限，两侧浪费 | 好 —— 左右各司其职 |
| 改动量 | 小（复用现有弹窗，只修三分区） | 大（新增双栏容器 + 状态提升 + 返回键语义） |

**为什么分两期**：一期就能把你现在的**阻塞问题**（横屏按钮消失、NPC 日程进不去）和**卡片等比放大**一次解决掉，且不引入大结构改动；二期才是"大屏该长什么样"的形态升级。二期不是必须，但既然在做大屏适配，二期才真正用上大屏的横向空间。

> ⚠️ **不是"分页"。** 没有任何翻页、Tab、分页器 —— 只是把"详情盖上去"变成"详情并排显示"。

### 6.1 触发条件（**二期生效**）

**双栏启用条件 = 宽度 ≥840dp `且` 高度 ≥480dp。**（✅ 已定）

| 场景 | 宽 | 高 | 是否双栏 |
| --- | --- | --- | --- |
| K40 竖屏 | 392dp | ~830dp | ❌ 单栏 |
| **K40 横屏** | 915dp | **393dp** | ❌ **单栏**（够宽但太矮 → 走三分区弹窗） |
| 平板横屏 | ≥840dp | ~600dp+ | ✅ 双栏 |
| Pura X Max 展开 | 940dp | 665dp | ✅ 双栏 |
| Pura X 内屏 | 440dp | 707dp | ❌ 单栏（宽度不够） |

> **为什么要加高度门槛**：手机横屏虽然"够宽"，但只有 393dp 高 —— 拆成左右两栏后，右栏详情的可视高度会非常局促，不如保持单栏 + 三分区弹窗舒服。这一条正好把"手机横屏"和"大屏横屏"干净地区分开，**无需引入任何设备类型判定**。
>
> **一期不启用双栏**（大屏仍用弹窗，见 6.4）。

> **不要**用"是否横屏"触发。Pura X Max 展开态是 **940×665 的横向窗口**，而平板竖屏 800dp 宽也可能是双栏的更优场景 —— **用宽度档，不用方向**。

### 6.2 布局形态

```
┌────────────────────────────┬──────────────────────────────────────┐
│  左：列表栏                  │  右：详情面板                          │
│  widthIn(min=300, max=380)  │  weight(1f)，内容 ≥600dp               │
│  ├ 网格（复用方案 A）        │  ├ 未选中 → 占位提示 / 空态            │
│  └ 滚动                     │  └ 选中 → 详情内容（自带滚动）          │
└────────────────────────────┴──────────────────────────────────────┘
```

### 6.3 关于"左 1/3 手机长屏比例"的评估（用户设想）

| 方案 | 840dp 屏 | 1200dp 屏 | 1600dp 屏 | 评价 |
| --- | --- | --- | --- | --- |
| 纯比例 `1/3` | 280dp（偏窄） | 400dp（尚可） | **533dp（过宽）** | ❌ 超宽屏失控 |
| 纯比例 `weight(0.30f)` | 252dp | 360dp | 480dp | ❌ 同上 |
| **夹紧区间 `widthIn(min=300, max=380)`** | **300dp** | **380dp（封顶）** | **380dp（封顶）** | ✅ **推荐** |

**结论**：用户设想的"左 1/3"在中等宽度屏上接近正确，但**必须改成固定区间而不是纯比例**，否则超宽屏左栏会失控。建议 **左栏 300–380dp**，右栏 `weight(1f)` 且保证 ≥600dp 可读宽。

### 6.4 ✅ 已定决策：**分两期**（一期弹窗 → 二期双栏）

| 期 | 大屏（≥840dp）详情形态 | 理由 |
| --- | --- | --- |
| **一期** | **保持弹窗**，但已完成三分区修复（热修 H0） | 先把 P0 阻塞解掉、网格自适应当期交付；双栏是大改动，单独一轮更稳、回滚粒度更细 |
| **二期** | **列表-详情双栏**（右栏为主详情） | 官方 list-detail 范式，横向空间利用率最高 |

其余形态不采用：BottomSheet 与 miuix 设计语言磨合成本高，且对"百科查询"这类需要大面积展示的场景不如双栏。

### 6.5 实现路径二选一

| 路径 | 说明 | 评价 |
| --- | --- | --- |
| **用库**：`NavigableListDetailPaneScaffold` | CMP commonMain 可用（见 3.6）；含导航 + 预测性返回 + 动画 | 省事，但引入 `@Experimental` API 与 alpha 版本依赖 |
| **自研**：`Row` + `AnimatedContent` + 状态提升 + `BackHandler` | 无新依赖；完全可控；与 miuix 设计语言自由结合 | 推荐（工程已有 `Navigator`，状态提升成本低） |

> 本工程已是单 Activity + 自研 `Navigator`（`ui/nav/Navigator.kt`），**自研双栏与现有导航模型更契合**；用库会引入一套并行导航语义（pane navigator vs 自研 Navigator），有语义冲突风险。

---

## 7. 方案 C：详情容器重构（P0 修复，必须先做）

### 7.1 目标模式：三分区（已落地 · `DialogSizing.kt` + 三弹窗）

> ⚠️ **与早期规划不一致**：方案稿写的是外层 `Column(fillMaxHeight(0.9f))`。**实际未用 `fillMaxHeight(0.9f)`**——那样会把短内容弹窗强制撑高。实际是外层 `Column(heightIn(max = rememberDialogMaxHeight(maxHeight)))`，`rememberDialogMaxHeight` 取 `min(原上限, 窗口高 × 0.9)`，**保留 640.dp 上限再二次收敛**。

```
Column (fillMaxWidth().heightIn(max = rememberDialogMaxHeight(maxHeight)))  ← 关键：限高按窗口高度收敛，不撑高短内容
├── header                            （固定高度，可随形态压缩）
├── 内容区  weight(1f, fill=false) + verticalScroll   ← 关键：weight 吃"剩余"且 fill=false，短内容不撑满
└── footer（按钮 Row，非加权，在 scroll 外）            ← 关键：结构不可挤出
```

**三个"关键"就是 P0 的完整修复（已提交 `8f38394` + `2819dc9`）**：

- 限高改用 `rememberDialogMaxHeight(maxHeight)`（`DialogSizing.kt`）→ `= Dp(min(max.value, windowHeight × 0.9))`：短内容弹窗按内容自适应收窄，长内容被窗口高 90% 兜住，横屏（≈393dp）不溢出；
- 内容区 **`weight(1f, fill = false)`** → Column 先测非加权的按钮行（拿自然高度），剩余才给内容；`fill = false` 保证短内容不撑满；
- footer **移出滚动区、且按钮 Row 非加权** → 结构上不可能被挤出。

### 7.2 落点清单

| 文件 | 当前 | 改为（已落地） |
| --- | --- | --- |
| `ui/components/BasicDetailDialog.kt` | `maxHeight=640.dp` 写死 + 内容列 + 按钮 Row | 三分区；`maxHeight` 经 `rememberDialogMaxHeight` 收敛；内容 `weight(1f, fill=false)`；按钮 Row 非加权移出滚动区；`OverlayDialog(largeScreen=false)` |
| `ui/npc/NpcDetailScreen.kt` | 总 640dp + 中部 400dp + header 144dp | 三分区；总高 `rememberDialogMaxHeight(640.dp)`；中部改 `weight(1f, fill=false)`；按钮各 `weight(1f)` 且 Row 非加权；`largeScreen=false` |
| `ui/items/ItemDetailScreen.kt` / `ui/recipe/RecipeDetailScreen.kt` | 内容不滚动，依赖容器 | 保持依赖容器即可（三分区后自动生效） |
| `ui/components/FilterPopup.kt`、`FilterChipDialog.kt`、`RecipeFilterDialog.kt` | 硬编码 600/480dp + 居中风险 | 统一走 `rememberDialogMaxHeight(...)` + `largeScreen=false` |

### 7.3 矮屏附加策略

- 横屏（高 <480dp）时，header 允许**横向化**（标题与返回/关闭同排），把纵向空间让给内容；
- 大屏（≥840dp）详情若走 pane（方案 B），则**根本不进 dialog**，P0 自然消失；
- 若保留 dialog，宽度用 `usePlatformDefaultWidth = false` + `widthIn(max = 560.dp)`，**避免大屏被拉满**。

### 7.4 强制底部贴合（已落地 · `2819dc9`）

**问题**：miuix `OverlayDialog` 在**宽≥840dp 且 高≥480dp**（即 `isLargeScreen()` 判定）时会改为 **`Alignment.Center`** 居中并对齐限高 `windowHeight × 2/3`，导致大屏/横屏上弹窗**距屏幕底部留白过大**（实测横屏约 229px vs 竖屏约 69px）。这是 **miuix 行为，不是本工程代码 bug**。

**修法**：给所有 `OverlayDialog` 显式传 **`largeScreen = false`**，强制走 `Alignment.BottomCenter`（贴底），使横屏与竖屏观感一致。

**涉及弹窗清单（全量，已 grep `largeScreen` 核实）**：

| 弹窗 | 文件 |
| --- | --- |
| 通用详情弹窗 | `ui/components/BasicDetailDialog.kt:53` |
| NPC 详情弹窗 | `ui/npc/NpcDetailScreen.kt:132` |
| 筛选弹窗（胶囊） | `ui/components/FilterChipDialog.kt:53` |
| 筛选弹窗（Popup） | `ui/components/FilterPopup.kt:50` |
| 配方筛选弹窗 | `ui/recipe/RecipeFilterDialog.kt:60` |

> 与 §7.1 的 `rememberDialogMaxHeight` 是两套独立修复：前者解决"限高 / 横屏按钮消失"，本节能决"横屏底部留白"。两者同属弹窗统一化，但根因不同。

---

## 8. 方案 D：方向策略（解决 Q7）

### 8.1 ⚠️ 先澄清一个关键误解：导航栏「旋转按钮」不能用来做这件事

用户设想：手机**默认竖屏**，用户点**导航栏那个旋转提示按钮**才切横屏。**这条路走不通。** 以下均为官方事实：

| 事实 | 说明 |
| --- | --- |
| 按钮的正确名称 | **Rotate suggestions（旋转建议）**，Android **9** 引入（**不是** Android 8，也不是 16/17 才有） |
| 触发条件 | 用户的**系统"自动旋转"被关闭**（旋转锁定生效）时，设备物理方向变化 → 导航栏出现该按钮 |
| 它服务谁 | 官方原文：是给「**本身能在多个方向渲染**」的 Activity 用的，用来把它转到用户当前手持的方向 |
| 对锁竖屏的应用 | 官方原文：「**要求特定方向的 Activity 会忽略用户锁定偏好设置**」→ `screenOrientation="portrait"` 把方向钉死，本身不提供其它旋转选项，**按官方逻辑该按钮不应出现** |
| 应用能否控制它 | **不能**。无任何公开 API 可让第三方 App 显示/隐藏该按钮（相关 `RotationPolicy` 是 `@hide` internal，调用方是 SystemUI / WindowManager） |
| 硬件导航键设备 | 官方明确：OEM「**自行实现或直接禁用**」→ 不可靠 |
| 国内 ROM | **未查到**权威证据，只有社区线索（小米有"单应用横屏模式"设置、三星有独立开关）→ **不能作为依赖项** |

> 【诚实标注】"portrait 锁定应用上该按钮一定不出现"是由两条官方描述**推导**而来，官方并无单句明文；**建议真机实测**。但即便它出现，也**不可控、ROM 不一致** —— 不足以支撑方案。**结论：不能复用系统按钮。**

### 8.2 三个可选策略

| 方案 | <sw600dp（手机） | ≥sw600dp（大屏） | 评价 |
| --- | --- | --- | --- |
| ① 保持自由旋转 | 自动跟随传感器 | 自由旋转 | 最简单，但做不到"默认竖屏" |
| ② Manifest 硬锁 `portrait` | ✅ 生效 | ❌ **无效**（targetSdk 37，Android 17 起无 opt-out） | 手机永远竖屏，**且连"想切"都切不了** → 与"横屏要保留"矛盾 |
| ③ **设置项开关 + 运行时 `setRequestedOrientation`**（✅ 已定） | ✅ 用户自选：关 = 锁竖屏，开 = 自由旋转 | ✅ 开关被系统忽略 → 走自适应 | **唯一同时满足"默认竖屏"+"横屏随时可切"的方案** |

**为什么 ③ 在手机上不被 Android 16/17 限制**：Android 16/17 的方向与可调整性限制，明确把「**显示屏尺寸 < sw600dp**」列入**例外**（大多数手机、竖折外屏）→ `setRequestedOrientation()` 在手机上**仍然有效**。

### 8.3 ✅ 已定决策（第二轮）：设置页加「手机横屏」开关

**做法：不改 Manifest 锁方向，改为一个应用内设置项 + 运行时方向控制。**

| 项 | 做法 |
| --- | --- |
| **设置项** | 新增 `allowPhoneLandscape: Boolean`（**仅影响手机**）；落点 = `utils/AppState.kt` + `platform/AndroidAppSettingsStore.kt`，沿用现有开关的持久化模式 |
| **关闭（锁竖屏）** | `activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)` |
| **打开（自由旋转）** | `activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)`（或 `FULL_SENSOR`） |
| **生效时机** | App 启动时应用一次；用户切换开关时**立即**应用 |
| **大屏** | Android 16/17 在 ≥sw600dp 上忽略方向请求（3.2）→ **开关自动失效，大屏横竖屏都走自适应**（与你的要求一致）。设置页该项加副标题"仅对手机生效" |
| **不重建** | `configChanges` 已含 `orientation\|screenSize` → 走 `onConfigurationChanged`，Compose 按 `WindowSizeClass` 重排 |
| **API 落点** | `setRequestedOrientation` 是 Android-only → 放 `androidMain` / Activity 层（单 Activity 架构很自然） |

**默认值建议：关（锁竖屏）** —— 与你"2400×1080 本来也不大、横屏更紧凑"的判断一致。想默认开也很容易翻。

> ⚠️ **官方警告必须遵守**：不能拿"反正不重建"当作回避配置变更的理由 —— **横屏布局必须真的正确**。这正是热修 H0（三分区）不可省的第二个理由。

---

## 9. 技术选型汇总

| 能力 | 选型 | 依赖 |
| --- | --- | --- |
| 窗口判定 | `LocalWindowInfo.current.containerDpSize` + 自算断点（`WindowClass.kt`，断点值同 AndroidX `WindowSizeClass`） | **零新增依赖**（`androidx.window` 未引入；`currentWindowAdaptiveInfo()` 已否决，理由见 §4.2.1） |
| 折叠避让 | `WindowInfoTracker` + `FoldingFeature` | 同上 |
| 自适应网格 | `BoxWithConstraints` + 自算列数 + `GridCells.Fixed` | 无（Compose 自带） |
| 列表-详情双栏 | **自研** `Row + weight + AnimatedContent + 状态提升`（备选：`NavigableListDetailPaneScaffold`） | 自研=0；用库=`org.jetbrains.compose.material3.adaptive:*` |
| 详情容器 | 三分区（`rememberDialogMaxHeight` 限高 + `weight(1f, fill=false)` + footer 非加权） | 无 |
| 方向 | **设置项「手机横屏」开关** + 运行时 `setRequestedOrientation`（默认关 = 锁竖屏）；大屏开关自动失效 | `AppState`/SettingsStore + `androidMain`（第 8.3） |
| 状态保存 | `ViewModel` / `SavedStateHandle`（`configChanges` 已挡重建，但进程回收仍需） | 视需要 |

**明确不采用**：`material3-window-size-class`（旧 artifact）、`GridCells.Adaptive`（无上限）、用 `sw600dp` 做布局分支、用 `FoldingFeature` 区分手机/平板。

---

## 10. 分阶段落地计划（**已定稿**）

> 已定节奏：**先热修 P0 → 一期（地基 + 网格）→ 二期（双栏 + 收尾）**。
> 原则：**先修阻塞，再做体验，最后做形态**。每阶段独立可验收、可回滚。
>
> **状态图例**：✅ 已落地（附 commit）｜🚧 进行中｜⏳ 未开始｜❌ 已作废 / 未落地。

### 🔥 热修 H0 · 详情弹窗三分区（立即 · 最高优先级）— ✅ 已落地 `8f38394`

**目标：立刻恢复 NPC 日程入口，解除阻塞。**（大屏锁不住 + 手机可点按钮切横屏 → 横屏仍属常规可达状态，此缺陷属高频复现）

1. `ui/components/BasicDetailDialog.kt`：`maxHeight` 经 **`rememberDialogMaxHeight`**（= min(640.dp, 窗口高×0.9)）收敛（非纯 `fillMaxHeight(0.9f)`）；内容列加 **`weight(1f, fill=false)`**；按钮 Row **移出滚动区且非加权**；`OverlayDialog(largeScreen=false)` 强制贴底；
2. `ui/npc/NpcDetailScreen.kt`：`:121-126` 总限高、`:131-137` 中部限高、`:140-148` 按钮 Row 同样改三分区；`:167` 的 header `144.dp` 在矮屏下允许压缩；
3. 三个筛选弹窗统一参数：`FilterPopup.kt:50`（600dp）、`FilterChipDialog.kt:53/87`（600dp）、`RecipeFilterDialog.kt:49/59`（480dp）。

- **验收**：K40 **横屏**（915×393dp）下，物品/配方/NPC 详情底部按钮**全部可见可点**、**NPC 日程可进入**；竖屏零回归；浅色/深色均正常。

### 一期 · 阶段 1：地基（不改用户可见行为）— ✅ 已落地 `1f98c6f` / `fc4d10a`

4. 新建 `shared/.../ui/adaptive/WindowClass.kt`：`WindowClass` 枚举 + `rememberWindowClass()` / `isShortWindow()`（第 4.2 节）；
5. ~~`shared/build.gradle.kts` **显式声明** `androidx.window`~~ ❌ **已作废 / 未落地**：实际改用 `LocalWindowInfo`（§4.2），**未引入 `androidx.window`**，`shared/build.gradle.kts` 无该声明（已 grep 核实）。
6. 新建 `AdaptiveIconGrid` 通用组件（第 5.2 节算法封装）。

- **验收**：编译通过；三列表替换为 `AdaptiveIconGrid` 后，**手机竖屏列数与现状完全一致**（K40 = 6 / 6 / 3 列，零视觉回归）。

### 一期 · 阶段 2：自适应网格参数化（按实测基线反推）— ✅ 已落地 `fc4d10a`

7. 按第 5.3–5.5 节接入三个列表：`minCard` 物品/配方 **47dp**、NPC **104dp**；规则 = "最大列数且单卡 ≥ minCard"，**填满整行**；仅当内容区 > 1280dp 才封顶居中。

- **验收**：K40 竖屏 **6 / 6 / 3 列不变、卡宽 54.7dp 与现状一致**；卡宽全程落在 **48–55dp**（NPC 100–133dp）；平板/大屏**不再等比放大**；**不出现两侧留白**；分屏、自由窗口拖拉宽度时实时重排不崩。

### 一期 · 阶段 3：手机方向设置项 — ✅ 已落地 `a1ffc84` + 修复 `70de16f`

8. 新增设置项 `allowPhoneLandscape`（`utils/AppState.kt` + `platform/AndroidAppSettingsStore.kt`），默认**关**；
9. `androidMain` 实现"按设置应用方向"：关 → `SCREEN_ORIENTATION_PORTRAIT`；开 → `SCREEN_ORIENTATION_UNSPECIFIED`。启动时应用一次 + 开关切换时立即应用（第 8.3 节）；
10. 设置页该开关加副标题「仅对手机生效」。

- **验收**：默认状态下手机**竖屏锁定**、晃动设备不会转横；打开开关后**恢复自由旋转**；关掉开关若当前是横屏应**自动回竖屏**；切换过程**状态不丢**；大屏上开关无效（横竖屏都正常自适应）。

### 二期 · 阶段 4：大屏双栏（宽 ≥840dp **且** 高 ≥480dp）— ✅ 已落地（形态改为「主页-子页分栏」）

> **形态变更（2026-09-14）**：6.0/6.4 原规划的是「物品/配方/NPC 三个板块子页内部各自拆左右栏」。
> 该方案**已实现并随后回退**（commit `07dc95f`）——它不是 Lawnchair 那种「左主干 + 右内容」的观感。
> 现行方案是 **`ListDetailPanes` 只做一层**：左栏 = 主页（3 板块 + 设置 + 底栏），
> 右栏 = 被点开的板块子页（`AppSubPageScaffold`），默认空态「暂无内容」。见 commit `1ed7a1e`。

11. ✅ `ui/adaptive/ListDetailPanes.kt`：`Row` + 左栏 `width = 窗口宽×0.34` 夹紧 `[300dp, 380dp]` + 右栏 `weight(1f)`；`rememberUseDualPane()` 判定（`rememberWindowClass()` ∈ {EXPANDED, LARGE} 且非矮屏）。**禁用 `sw600dp` 判定、禁用 `FoldingFeature` 判定设备类型**。
12. ✅ 不满足条件（含**手机横屏 915×393dp**）保持单栏 + 弹窗（H0 已修好）。
13. ✅ 返回键语义：`DualPaneBackHandler(enabled = paneBackStack.size > 1)` —— 右栏有子页时先退右栏，空态时 `enabled=false` 自动让位给 `NavDisplay`。
14. ✅ **右栏用独立返回栈**：`rememberNavBackStack<Route>(Route.Main)` + 包一层 `Navigator`，通过 `LocalNavigator` 下发。好处是各业务页面里现成的 `navigator.push(...)` / `navigator.pop()` **一行都不用改**；`rememberNavBackStack` 自带 saver，旋转 / 改分辨率不丢右栏内容（实测 `wm size` 改动后右栏仍是原页面）。
15. ✅ **导航语义拆分**（commit `cd1f989`）：
    - `Navigator.openTopLevel(key)` —— **左栏平级跳转**：先清栈到栈底再入栈（主页三板块、设置页的「关于 / 图片倍率」）。修复「连点板块无反应」：左栏常驻 ⇒ 右栏栈会累积成 `[Main, ItemList, RecipeList, NpcList]`，此时再点「物品大全」被 `push` 的幂等跳过。
    - `Navigator.push(key)` —— **右栏层级深入**：NPC 资料 → 日程、关于 → 协议，这类才该叠上去。
16. ✅ **弹窗归属右栏**（commit `e06a77d`）：**不需要任何偏移代码**。`OverlayDialog` 默认 `renderInRootScaffold = true`，弹窗 + 遮罩渲染进最近的 miuix `Scaffold`；右栏子页自带 `AppSubPageScaffold` ⇒ 天然以右栏为基准居中、遮罩只盖右栏。
17. ✅ **左栏自补遮罩 + 弹窗优先**（commit `d50996c`）：因为遮罩只盖右栏，左栏在弹窗期间完全没被覆盖 ⇒ 点左栏会**直接跳转把弹窗盖掉**。`ui/adaptive/DetailOverlayState.kt` 让弹窗登记进 `LocalDetailOverlay`，左栏据此补一层等价遮罩并把点击解释为 `dismissTop()`；`DIALOG_CLOSE_GRACE_MS = 280` 覆盖关闭动画（miuix `dimProgress` 淡出 250ms），避免第二击「穿过」动画尾巴。

- **验收**：平板横屏、Pura X Max 展开态（940×665）下左主干 + 右内容同屏；**手机横屏（915×393dp）保持单栏**、不走双栏。

#### 实测证据（模拟器 emulator-5568）

| 窗口（dp） | 左栏规则 | 右栏中心（预测） | 弹窗卡片中心（实测） | 偏差 |
|---|---|---|---|---|
| 1105×726 @2.05 | 比例 375.7dp | 1518.0px | **1518.0px** | 0.0 |
| 1280×800 @2.0 | 上限夹紧 380dp | 1660.0px | **1659.5px** | −0.5 |
| 1000×720 @2.0 | 比例 340dp | 1340.0px | **1339.5px** | −0.5 |

- 三个尺寸下弹窗卡片宽度恒为 **840px = 420.0dp**（= `DialogDefaults.MaxWidth`）。
- 弹窗打开时左栏底色 247→172（比值 0.696）、右栏 255→178（0.698）—— 两栏遮罩浓度一致。
- **结论：居中完全由容器推导，工程内不存在任何「偏移值」，换屏幕尺寸无需改代码。**

#### 仍未验证

- **跨断点行为**：横屏进右栏子页后转竖屏（单栏），右栏栈被 saver 保留但**不再渲染** ⇒ 回横屏会恢复；竖屏期间该内容不可达，是否符合预期待确认。

### 二期 · 阶段 5：收尾 — ⏳ 未开始

14. 折叠姿态（Tabletop / Book）避让 —— **可选**，视是否有折叠设备；
15. 状态保存复核（`ViewModel` / `SavedStateHandle`；因 `configChanges` 挡了重建，旋转不丢 UI 状态，但进程回收仍需兜底）。

---

## 11. 验收清单

### 11.1 功能

**方向（P0）**
- [ ] 设置项「手机横屏」默认**关** → 手机竖屏锁定，晃动设备**不会**自动转横
- [ ] 打开设置项 → 恢复自由旋转；关掉时若正处横屏 → **自动回竖屏**
- [ ] 切换开关过程中**状态不丢**（当前页、选中项、滚动位置）
- [ ] 大屏上该开关**无效**（横竖屏都正常），且大屏横屏仍能正常显示

**详情容器（P0）**
- [ ] 手机横屏（~915×393dp）：物品/配方/NPC 三个详情底部按钮**均可见可点**；**NPC 日程可进入**
- [ ] 竖屏（K40 392dp）：三个详情观感与 v28 一致、零回归

**网格自适应（目标：卡宽全程 48–55dp；NPC 100–133dp）**
- [ ] 360dp 小屏：物品/配方 6 列 · 49.3dp；NPC 3 列 · 104dp
- [x] 392dp（K40）：6 / 6 / 3 列 · 54.7dp / 114.7dp（= 现状）✅（唯一有实测基线的宽度档）
- [ ] 411dp：物品/配方 7 列 · 48.4dp
- [ ] 440dp（Pura X 内屏 440×707）：7 列 · 52.6dp；NPC 3 列 · 130.7dp
- [ ] 326×326（Pura X 外屏方屏）：物品/配方 5 列 · 54dp，UI 不挤压
- [ ] 600dp 平板竖屏：10 列 · 50.4dp；NPC 5 列 · 105.6dp
- [ ] 840dp 平板横屏：14 列 · 50.9dp；NPC 7 列 · 106.3dp
- [ ] 1200dp 桌面窗口：21 列 · 48.4dp；NPC 10 列 · 106.8dp
- [ ] **任何宽度下都不出现两侧留白**（不设列数上限、不设内容区限宽）
- [ ] 卡片**没有等比放大**（大屏卡宽 ≤55dp）
- [ ] NPC 卡片**宽度与高度同步变化**（因立绘区为正方形 `aspectRatio(1f)`）

**大屏双栏（二期 · 形态=主页-子页分栏）**
- [x] 宽 ≥840dp **且** 高 ≥480dp → 左主干 + 右内容同屏 ✅（1105×726 / 1280×800 / 1000×720 三档实测）
- [x] **手机横屏（915×393dp）不触发双栏**，保持单栏 + 弹窗 ✅（竖屏 726dp 实测单栏无回归）
- [ ] 平板竖屏 600dp（宽不够）→ 单栏；Pura X 内屏 440dp → 单栏
- [x] Pura X Max 展开态（940×665）→ 双栏生效，右栏详情可滚动、按钮可见（同类档位 1000×720 已实测）
- [x] 大屏返回键 = 先退右栏，右栏空态时才交给路由返回
- [x] 右栏内容以 `rememberNavBackStack` 持有 ⇒ 改分辨率 / 旋转后不丢（实测 `wm size` 改动后右栏仍是原页面）
- [x] 左栏连点不同板块「先清栈再入栈」，不会因幂等跳过而**无反应**
- [x] 弹窗**以右栏为基准自动居中**（实测 3 档尺寸，卡片中心 = 右栏中心，偏差 ≤0.5px），且**工程内无任何偏移常量**
- [x] 弹窗开着时点左栏 = **先关弹窗**，第二击才跳转；关闭动画期间（280ms）持续拦截
- [x] 左栏遮罩浓度与右栏 miuix 遮罩一致（底色比值 0.696 vs 0.698），时长曲线对齐 `DialogContentLayout`
- [ ] 横屏进右栏子页 → 转竖屏 → 再转回横屏，右栏内容是否按预期恢复

### 11.2 健壮性
- [ ] 分屏（左右各占一半）下布局正确，不依赖 `sw` 值
- [ ] 自由窗口拖拉改变宽度时**实时**重排，无崩溃
- [ ] 旋转时不丢状态（当前选中项、滚动位置）
- [ ] 深色 / 浅色模式均正常（v28 刚修过卡片底色，勿回归）

### 11.3 不回归
- [ ] `composeResources` 红线未触碰（`:shared` commonMain 仍不得用 `Res.drawable.*`）
- [ ] 未引入新的 `OverlayDialog + 滚动` 无限高崩溃
- [ ] 未使用 `material3-window-size-class` 旧 artifact
- [ ] 黑名单 / 类别合并（#27）等数据逻辑零影响

---

## 12. 风险与未决项

| 风险 | 等级 | 说明 / 缓解 |
| --- | --- | --- |
| miuix 的 `isLargeScreen()` 阈值（480×840）与我们的判定不一致 | 中 | 两套判定并存可能出现"我们判大屏、miuix 判小屏"的错配。**缓解**：详情改三分区并经 `rememberDialogMaxHeight` 限高（= min(原上限, 窗口高×0.9)），不再依赖 miuix 的定位/限高分支；必要时同步调整 miuix 库（该库为本地 `includeBuild`，**可改**） |
| miuix `OverlayDialog` 的定位行为（2.3 的空档问题） | 中 | 需真机截图确认；三分区重构后复验 |
| `NavigableListDetailPaneScaffold` 为 `@Experimental` + alpha | 中 | **缓解**：选自研双栏路径，规避 |
| 国内 ROM 覆盖手机方向锁 | 低 | 接受现状；保证横屏可用 |
| 无真机/模拟器覆盖全部形态 | 高 | **缓解**：用 `adb shell wm size` / `wm density` 模拟多档尺寸做验证；关键形态需用户真机复核 |
| 工程无自动化 UI 测试 | 中 | 本方案的验收**依赖人工真机**，需用户配合逐档核对 |

### 未决项（需用户确认）

1. **§5.3 K40 密度歧义**（392dp vs 429dp）—— 需你确认；**px 结论（151px / 316px）不受影响**；
2. **§8.1「portrait 锁定应用上 rotate-suggestion 按钮一定不出现」** —— 由两条官方描述**推导**而来，官方无单句明文；**建议真机实测**（Pixel + 小米各一台）。但结论（不能依赖它）不因此改变；
3. §3.1「`material3-window-size-class` 是否官方废弃」—— 调研**未查到官方原文**，方案按"不再使用"执行；
4. §3.4「国内 ROM 在手机上忽略 `portrait` 锁定」—— **无官方文档级证据**，仅有社区证据，方案按"可能被覆盖"做保守设计；
5. ~~§5.5 NPC 卡宽波动~~ —— **已确认接受**（手机段稳定 3 列，卡宽 104–131dp，相对基准 −9% ~ +14%）；
6. §2.3 的 dialog 底部空档 —— 需真机确认根因。

---

## 13. 已定决策（2026-09-13 主理人拍板 + 同日第二轮修订）

> **本文件自本节起为实施基线。**（2026-09-13 经两轮拍板）

| 决策 | 结论 | 落地位置 |
| --- | --- | --- |
| **大屏形态** | **分两期**：一期弹窗（三分区）→ 二期列表-详情双栏（含义见 6.0） | 第 6 节、第 10 节 |
| **双栏触发** | **宽 ≥840dp `且` 高 ≥480dp** → 手机横屏（915×393dp）**不**触发双栏 | 第 6.1 节 |
| **手机方向** | **设置页加「手机横屏」开关**（运行时 `setRequestedOrientation`），**默认关**；大屏该开关自动失效 | 第 8.3 节 |
| **系统旋转按钮** | ❌ **不采用**（无公开 API 可控、ROM 不一致） | 第 8.1 节 |
| **卡片参数** | 物品/配方 `minCard 47dp`（基线卡宽 54.7dp = 151px）；NPC `minCard 104dp`（基线 114.7dp = 316px）；规则 = "单卡 ≥ minCard 的**最大列数**" | 第 5.3–5.5 节 |
| **列数上限** | **不封顶**、不设内容区限宽（永不出两侧留白；代价是超宽屏列数多） | 第 5.5 节 |
| **落地节奏** | **热修 H0 → 一期（地基 + 网格 + 方向开关）→ 二期（双栏 + 收尾）** | 第 10 节 |

### 13.1 决策带来的强化项

1. **热修 H0 仍是最高优先级** —— 虽然手机默认竖屏，但 ①**大屏锁不住**（targetSdk 37）、②用户**可打开设置项**随时进横屏、③ROM 可能覆盖 —— 横屏仍必须能看，横屏下按钮被挤出是"一开就复现"的缺陷；
2. **K40 横屏 = 915×393dp 是本次的关键测试档**：它**够宽（≥840）但太矮（<480）** →
   - 二期双栏必须验证它**不触发**双栏；
   - 三分区弹窗必须验证它在 393dp 高度下按钮仍可见、NPC 日程可进入；
   - 网格按 915dp 宽算列数会得到约 **16 列**，需确认观感可接受；
3. 验收清单 11.1 的**横屏相关项必须逐条真机过**，不能靠"竖屏没问题"推断；
4. **超宽屏列数需实测观感**：1200dp → 21 列属"已确认接受"，但**建议先看过再定**（可用 `adb shell wm size` 改尺寸模拟，无需真机）。

### 13.2 因决策而关闭的备选项

- ❌ **复用系统导航栏旋转按钮** —— 不可控、ROM 不一致，且官方逻辑上对"锁定方向的应用"本就不该出现（8.1）；
- ❌ 手机"完全自由旋转" —— 改为默认竖屏 + **应用内**旋转按钮；
- ❌ 大屏一次性上双栏 —— 改分两期（6.0）；
- ❌ 卡片"大卡 / 小卡"预设 —— 改为**以实测基线 + ±20px 反推**参数（5.3–5.5）；
- ❌ BottomSheet 作为大屏详情形态 —— 不采用。

---

## 14. 已知偏差与遗留

> 本轮把文档与已提交实现（`git log` 见 §10）对齐，以下为**如实记录的偏差与遗留**，供下一轮避免重复规划、改坏已验代码。

1. **判定 API 改用 `LocalWindowInfo.containerDpSize`，未用 `currentWindowAdaptiveInfo()`**：断点值（1200/840/600.dp，从大到小）与 AndroidX `WindowSizeClass` 一致，但实现路径不同。原因见 §4.2.1（`material3-adaptive` 不在本地缓存 + `:shared` 单 `androidLibrary` target 的 metadata 风险）。
2. **未引入 `androidx.window` 依赖**：`shared/build.gradle.kts` 无 `androidx.window` 声明（已 grep 核实）；窗口判定零新增依赖。
3. **「手机横屏」开关曾在 `a1ffc84` 漏写 `load()` 读取，导致重启后失效**，已在 `70de16f` 修复（QA 发现）。
4. **`rememberWindowClass()` / `isShortWindow()` 目前尚无调用方**：仅作为阶段 4（大屏双栏）的地基预埋，待二期接入。
5. **构建环境红线**：本机仅 13GB 内存，`gradle.properties` 的 `-Xmx8g`（`org.gradle.jvmargs`）在该配置下可能导致 JVM 原生 OOM；且增量构建可能出现「全部 UP-TO-DATE 的假 BUILD SUCCESSFUL」——验证必须看 `compileAndroidMain` / `compileDebugKotlin` 是否**真执行** + 比对 APK 的 mtime，不能只看末尾 BUILD SUCCESSFUL。
6. **二次核实**：本文档 §5.2 与 §5.3–5.5 的矛盾已在本轮修正（§5.2 标注为早期草案、已被取代）；§4.2 / §7 / §9 中与实现不符的描述已同步修正。

---

## 附录 A：调研来源

**官方**
- <https://m3.material.io/foundations/layout/breakpoints>
- <https://developer.android.com/develop/ui/views/layout/use-window-size-classes>
- <https://developer.android.com/reference/kotlin/androidx/window/core/layout/WindowSizeClass>
- <https://developer.android.com/guide/topics/large-screens/support-different-screen-sizes>
- <https://developer.android.com/guide/topics/large-screens/make-apps-fold-aware>
- <https://developer.android.com/develop/ui/compose/layouts/adaptive/canonical-layouts>
- <https://developer.android.com/develop/ui/compose/layouts/adaptive/list-detail>
- <https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive>
- <https://developer.android.com/about/versions/16/behavior-changes-16>
- <https://developer.android.com/about/versions/17/changes/ff-restrictions-ignored>
- <https://developer.android.com/about/versions/pie/android-9.0>（**rotate suggestions 旋转建议按钮**引入于 Android 9）
- <https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/grid/GridCells.Adaptive>
- <https://developer.android.com/reference/android/R.attr#screenOrientation>（含 Android 12+ 制造商可忽略方向指定；以及"要求特定方向的 Activity 会忽略用户锁定偏好"）
- <https://developer.huawei.com/consumer/cn/doc/doccenter-multi-device/bpta-purax-guide>
- <https://www.jetbrains.com/help/kotlin-multiplatform-dev/whats-new-compose-170.html>

**社区**
- <https://stackoverflow.com/questions/67514215>（dialog 高度/按钮被挤出）
- <https://stackoverflow.com/questions/70622649>
- <https://m.xiaomitong123.com/baike/12809.html>（澎湃OS 单应用横屏模式）

## 附录 B：代码证据索引

| 证据 | 路径:行 |
| --- | --- |
| 物品列数硬编码 | `shared/src/commonMain/kotlin/com/nainiuzhen/wiki/ui/items/ItemListScreen.kt:126` |
| 配方列数硬编码 | `shared/src/commonMain/kotlin/com/nainiuzhen/wiki/ui/recipe/RecipeListScreen.kt:128` |
| NPC 列数硬编码 | `shared/src/commonMain/kotlin/com/nainiuzhen/wiki/ui/npc/NpcListScreen.kt:121` |
| 详情内容列 + 按钮 Row | `shared/src/commonMain/kotlin/com/nainiuzhen/wiki/ui/components/BasicDetailDialog.kt:37,46-51,55-63` |
| NPC 三重限高 | `shared/src/commonMain/kotlin/com/nainiuzhen/wiki/ui/npc/NpcDetailScreen.kt:121-126,131-137,140-148,167` |
| NPC 日程入口 | `NpcDetailScreen.kt:83` → `ui/nav/Route.kt:35` → `ui/nav/AppNavHost.kt:57` → `ui/npc/NpcScheduleScreen.kt:63` |
| miuix 大屏判定 | `miuix/miuix-ui/src/commonMain/kotlin/top/yukonga/miuix/kmp/layout/DialogContentLayout.kt:422-427,295-296,351` |
| Manifest | `app/src/main/AndroidManifest.xml` |
| SDK 版本 | `app/build.gradle.kts:10,18,19` |
| miuix 依赖 | `shared/build.gradle.kts:25-30`、`settings.gradle.kts:36` |
