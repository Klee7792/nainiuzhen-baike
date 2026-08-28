# 奶牛镇百科 App — build #4 修复总结

> APK：`builds/nainiuzhen-baike_v4_debug.apk`（已存盘 + git tag `build-4`，可回退）
> 编译：`assembleDebug` BUILD SUCCESSFUL（Gradle 9.6.1 / AGP 9.3.2）

## 按用户反馈逐项修复

### 1. 加载后闪退（根因）
- 真凶：`OverlayDialog` 用 **Infinity 最大高度**测量内容，弹窗里的 `verticalScroll`/`ExpandableRichText` 被无限高度测崩（`IllegalStateException: ... measured with an infinity maximum height`）。
- 修复：所有详情弹窗 body 根容器统一加 `Modifier.heightIn(max = 640.dp)` 收敛约束；删掉会崩溃的「展开/收起」按钮，统一为「关闭」按钮。

### 2. 导航结构：3 板块归主页 + 设置底栏第 2 页
- `Route.Home` → `Route.Main`；`MainScreen` 用 `NavigationBar` + 两个 `NavigationBarItem`（主页 / 设置）。

### 3. 物品大全 = 6 列方块网格
- `LazyVerticalGrid(GridCells.Fixed(6))` + 吸顶「共 N 个物品」+ 多选分类 chip；物品弹窗按 PRD 4 区块：名称售价 + 白/金/紫星倍率卡片（四舍五入取整）、描述（限高可滚）、来源、关闭。

### 4. 切片旋转方向（种子袋开口朝下 → 朝上）
- `AndroidSpriteSlicer` 由 `postRotate(90)`（CW，错）改为 `postRotate(-90)`（CCW，对）；`versionCode=2` 强制重建切片缓存。

### 5. 配方弹窗按 PRD
- 名称（无售价）/ 描述 / 解锁条件 / 原料区（≤4 居中、超出横滚）/ 产物区 / 关闭；原料·产物卡片点击**穿透出物品详情**（嵌套 OverlayDialog）。

### 6. NPC 资料：3 列网格 + 6 区块详情
- 列表 3 列网格；详情：立绘(约80%) + 住址/生日/好感max / 故事 / 好感度(loveItemId) / 最爱·喜欢·讨厌（空也占位保持高度）/ 日程+关闭（好感max=0 时日程禁用）；最爱等卡片点击穿透物品详情。

### 7. 设置：完整中文化 + 记忆
- 恢复 miuix 风格设置项（深色模式 / 启用圆角 / 启用模糊 / 过渡动画 / 清理缓存 / 版本）；`AppSettingsStore`(SharedPreferences) 持久化，深色模式重启保留。

### 8. 日程：已婚/未婚筛选 + 时间轴
- 筛选：星期 / 天气 / 季节 / 婚姻（未婚=0 / 已婚=1）单选必选、**无「全部」**，默认 周一/晴天/春/未婚；
- 布局：左侧开始时间列 + 右侧名称（大字）+ `起点 → 终点` 场景箭头（小字），条目等高。

## 编译踩坑（已记入项目 MEMORY.md，避免复发）
- **`weight` 显式 import 会解析到内部属性** → 分栏改用 `fillMaxWidth(0.49f)` + `Arrangement.SpaceBetween` / 固定 `width()`。
- **`LazyRow` 内 `items()` 与网格 `items()` 重载冲突** → 列表内改用 `forEach { item { } }`。
- **`NavTransitions.AOSP` 不存在** → 用 `NavTransitions.Modal`。
- **`MiuixIcons.Home/Settings` 需 `icon.extended` import**。
- **`Float.roundToInt()` 自定义扩展返回 Double** → 改用 stdlib `kotlin.math.roundToInt`。

## 仍需用户侧验证
本沙箱无法启动 Android 模拟器，运行期 UI/真机表现需用户安装 `nainiuzhen-baike_v4_debug.apk` 后确认：
- 各弹窗不再闪退；种子袋开口朝上；深色模式重启保留；
- 6 列网格、3 列 NPC、弹窗布局、日程筛选与时间轴符合预期。
