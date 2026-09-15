# iOS 版移植规划（无 Mac · CI 出包 · 巨魔侧载）

> 2026-09-15 立项。目标：基于现有 CMP 工程产出**未签名 ipa**，经 GitHub Actions macOS runner 构建，
> 由 TrollStore（巨魔）侧载到真机验证。全程不依赖本地 macOS、不做签名、不上架。

---

## 0. 目标与边界

| 项 | 决定 |
|---|---|
| 构建方式 | 仅 GitHub Actions `macos-latest`（无本地 Mac，Windows 本地只改代码） |
| 产物 | 未签名 `.ipa`（`CODE_SIGNING_ALLOWED=NO`），Actions artifact 下载 |
| 安装方式 | TrollStore 侧载（免签名，正是巨魔场景） |
| 素材 | 私有仓 `Klee7792/nainiuzhen-assets` main 分支 `assets.pack`（NZPK v2），CI 用 `ASSETS_PAT` 拉取后打进 app bundle |
| 不做 | App Store 上架、TestFlight、描述文件/证书、Mac 模拟器调试 |

## 1. 架构现状（2026-09-15 已逐项核实）

- **`shared/commonMain` 90+ 文件 = 全部业务与 UI**：三板块列表/详情、设置与 About、双层导航
  （MainScreen/SubPageNavHost/ListDetailPanes）、液态玻璃底栏、跑马灯、卡片外观契约、
  plist 解析（PlistParser）、富文本（RichTextParser）、DataRepository——**iOS 一行不用改，直接复用**。
- **Android 专属代码极少**：`:app` 模块只有 `MainActivity`（装配层）；`shared/androidMain` 共 10 个文件。
- **平台隔离点**：5 个 expect（`readAssetBytes` / `ApplyPhoneOrientation` / `SetStatusBarLightIcons` /
  `OnResumeEffect` / `appStartElapsedMs`）+ 3 个平台接口实现（`SpriteSlicer` / `CacheProvider`+`SpriteCacheManager` / `AppSettingsStore`）。
- **miuix 全家桶（ui/nav/preference/blur/squircle/icons，已 vendor）全部自带 `iosArm64()` + `iosSimulatorArm64()` target**。
- 版本栈：AGP 9.4.0 / Gradle 9.6.1 / Kotlin 2.4.10 / **Compose Multiplatform 1.12.0**（iOS 支持成熟）。
- CI 已有 `ios` 占位 job（`macos-latest` + `shared/src/iosMain` 存在性检查），素材拉取步骤可直接复制 Android job 的写法。

## 2. 分阶段路线图

### Phase 1 — iOS target 冒烟（先验证、不实装）

**改动最小、只做配置**，目的：用 CI 精确摸清 commonMain 在 K/N 下的真实编译缺口。

1. `shared/build.gradle.kts`：加 `iosArm64()` / `iosSimulatorArm64()`（要不要 `iosX64()` 待定，CI 无 Intel runner 价值不大）。
2. 建 `shared/src/iosMain/kotlin/` 骨架，先只放 `Platform.kt` 空壳（不放 actual，让编译器把缺的 actual 全部报出来）。
3. CI 临时跑 `:shared:compileKotlinIosArm64`（或临时 gradle task），收集编译错误清单。

**⚠️ Windows 本地回归检查（必做）**：
- 加 target 后，本地 `clean assembleRelease` 走 `--configure-on-demand` 不得被 K/N 配置牵连
  （重点盯：依赖解析是否触发 konan 下载、配置阶段是否变慢/报错）。
- 若牵连，则 iOS target 配置须做成「仅 CI 生效」（如 `-PenableIos` 属性门控）。
- 验收：本地 v38 构建（注意 memory 中构建脚本 git 段要先修）照常出 APK，且配置阶段耗时无显著回退。

### Phase 2 — iosMain 平台实现（核心工作量）

按依赖顺序逐项落地（Android 侧代码一律不动）：

| # | 项 | 方案 | 难度 |
|---|---|---|---|
| 1 | `PackDecoder` 下沉 commonMain | parse / XOR / FNV-1a64 均是纯 Kotlin 可直接搬；`ByteBuffer` 改手写小端读取；`java.util.zip.Inflater` → expect `inflateRaw()`：Android actual 用 `Inflater`，iOS actual 用 K/N 预置 `platform.zlib`（iOS SDK 自带 zlib，uncompress 即可）。**改动须与 `tools/pack_assets.py` 格式注释保持同步** | 中 |
| 2 | `readAssetBytes` iOS actual | 先查 pack（bundle 内 `assets.pack`），再回退 `NSBundle.main.path(forResource:ofType:)` 逐目录找。PackDecoder 的「Android 专用」加载入口（`AppContextHolder`）需抽象成 expect 的 pack 来源 | 中 |
| 3 | `SpriteSlicer` iOS actual | skiko（CMP 自带）：`Image.makeFromEncoded` → `Bitmap` 裁剪/旋转 → 贴回 sourceSize 画布 → `Image.encodeToData` 编 PNG。**rotated（Cocos 顺时针 90°）/ sourceColorRect 偏移逻辑逐条对齐 AndroidSpriteSlicer.kt 与 `smart_picture_tool.py` 的 `_slice_plist_new`**；`sliceBatch` 同样单次解码 | 中高（对齐精度是硬要求：像素艺术硬边、零模糊） |
| 4 | `CacheProvider` / `SpriteCacheManager` iOS | 缓存目录 = `NSHomeDirectory() + /Library/Caches`（bundle id 子目录），版本号用 `CFBundleShortVersionString`+build 或与 Android 同源的常量注入 | 小 |
| 5 | `AppSettingsStore` iOS | `NSUserDefaults`（suite `com.nainiuzhen.wiki`），接口是 commonMain 的，只写 actual | 小 |
| 6 | `OnResumeEffect` iOS | `NSNotificationCenter`：`didBecomeActive` / `willResignActive` 映射 resume/pause | 小 |
| 7 | `appStartElapsedMs` iOS | `clock_gettime`（posix）或模块加载时间戳 | 小 |
| 8 | `SetStatusBarLightIcons` iOS | 调研点：CMP iOS 的窗口/VC 归属；大概率经 `preferredStatusBarStyle`（宿主 VC）或 `overrideUserInterfaceStyle`。dark/light 主题切换时由 App.kt 现有调用点驱动 | 中（需真机试） |
| 9 | `ApplyPhoneOrientation` iOS | 调研点：CMP iOS 设置 `supportedInterfaceOrientations` 的通路；兜底方案是 Info.plist 全局锁竖屏 + 大屏判断逻辑留在 commonMain（当前 `smallestScreenWidthDp>=600` 的分支语义要映射成 iOS 的 size class） | 中 |
| 10 | Toast / 返回手势 / 弹窗 | `AppToast` 是 commonMain Compose 实现，预计免改；返回手势 iOS 无系统侧滑返回（我们没接 predictive back，影响小），Dialog 关闭已用自定计时器，预计免改 | 小 |

**验收**：CI 上 `:shared:linkReleaseFrameworkIosArm64` 产出 framework；编译 0 错误。

### Phase 3 — iosApp 壳工程 + CI 出包

- 新增 `iosApp/`（标准 CMP 模板 Xcode 工程）：极薄 Swift 入口，调 `App(slicer=IosSpriteSlicer(), cache=..., settings=..., ...)`——与 MainActivity 同构。
- `Info.plist`：`CFBundleDisplayName=奶牛镇百科`、竖屏声明、`UILaunchScreen`（纯色即可）、最低版本（见开放问题 2）。
- 图标：现成 `ic_launcher.png` 转全套 AppIcon（含 1024）。注意该图标**不在公开仓**（assets 根散文件被过滤）——CI 拉素材时一并处理，或提交一份从私有仓导出的图标到 iosApp（不涉及游戏素材，可进公开仓，需确认）。
- CI job（改造现有占位）：`macos-14` + JDK21 + setup-gradle 9.6.1 → PAT 拉 `assets.pack` 放进 iosApp 资源 →
  `gradle :shared:linkReleaseFrameworkIosArm64`（Gradle 侧产出 framework）→
  `xcodebuild -sdk iphoneos CODE_SIGNING_ALLOWED=NO ... build` → 手工组 `Payload/*.app` → `zip` 成未签名 `.ipa` → artifact。
- 版本号：与 Android 版本戳（`1.(N/10).(N%10)`）同源注入 `CFBundleShortVersionString` / `CFBundleVersion`。

**验收**：Actions 产出 ipa，巨魔安装后**能启动、能出数据**（哪怕 UI 瑕疵一堆）。

### Phase 4 — 真机（巨魔）验证与 iOS 化打磨

- 巨魔直接装未签名 ipa（无需签名/重签）。
- **无模拟器的调试策略**：应用内加一个 debug 日志浮层/导出日志按钮（可复用设置页 Debug 区思路），把启动切片预热耗时、缺失资源路径等打到可截图的地方——这是无 Mac 下最重要的排障手段。
- 验证清单（沿用像素级验证思路）：
  1. 冷启动 → 数据加载（5125 物品 + 黑名单过滤）与切片预热耗时；
  2. 图标渲染：最近邻硬边、零模糊、整倍数居中（Skia 下确认 `FilterQuality.None` 生效路径）；
  3. 安全区：刘海/灵动岛/Dynamic Island 与底部 home indicator 的 padding；
  4. 深色模式 + 状态栏图标明暗；
  5. 三板块卡片外观设置（背景/圆角/胶囊/字号）持久化（NSUserDefaults）；
  6. miuix-blur 液态玻璃底栏、About 背景特效在 iOS Skia 的实际渲染质量；
  7. 双层导航 + 分栏（大屏/横屏语义在 iOS 上的 size class 映射）；
  8. 内存与滚动流畅度（K/N GC 对长列表的影响）。

## 3. 已知风险与未知

| 风险 | 说明 | 缓解 |
|---|---|---|
| 无模拟器调试 | 所有 UI 验证 = CI → 巨魔装包 → 真机截图，迭代慢 | Phase 4 的日志浮层先行 |
| K/N zlib 绑定 | `platform.zlib` 在 apple targets 是否开箱可用待验证 | 备选：自实现 raw inflate（仅 deflate 级别）或换第三方 |
| blur / BgEffect 在 iOS 的表现 | miuix-blur 有 iOS target，但实际渲染质量（尤其 About 页特效）未知 | Phase 4 专项验证，不行就 iOS 上降级关闭 |
| CMP 1.12 iOS 边角 | expect/actual API 细节、键盘、文本输入在 iOS 的行为 | 逐项真机验证 |
| Windows 本地回归 | 加 target 可能牵连配置阶段 | Phase 1 的 `-PenableIos` 门控预案 |
| xcodebuild 无人值守 | 签名关掉后一般无坑，但 provisioning / entitlements 残留会报错 | CI 脚本显式 `CODE_SIGNING_ALLOWED=NO CODE_SIGN_IDENTITY=-` |

## 4. 开放问题（动手前确认）

1. **Bundle ID**：跟游戏包一致 `com.harvesttown.encyclopedia`，还是独立 `com.nainiuzhen.wiki.ios`？（巨魔不校验，仅影响本机唯一性与未来上架。）
2. **设备 iOS 版本**：巨魔机具体 iOS 版本决定 deployment target 下限（建议 target iOS 14.0；若设备 15+/16.x 可再放宽）。
3. **版本号策略**：iOS 与 Android 共用 `build_number.txt` 递增，还是独立计数、版本名同步？
4. **图标入仓**：app 图标（非游戏素材）是否允许进公开仓给 iosApp 用？还是也从私有仓拉？
5. **Phase 1 是否同批 push**：配置改动是否与本文档一起 push，push 后立即触发 CI 冒烟？

## 5. 现在做什么

- [ ] 本文档 review（开放问题 1–5 给结论）
- [ ] Phase 1：iOS target 配置 + iosMain 骨架 + CI 冒烟（先修 `builds/_v37_build.sh` git 段，见 memory 红线）
- [ ] Phase 2：按上表 #1→#5→#3→#4 顺序实装（数据链路优先于图像链路）
- [ ] Phase 3：壳工程 + CI 出包
- [ ] Phase 4：真机验证清单逐项过
