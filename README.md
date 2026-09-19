# 奶牛镇百科 App（nainiuzhen-baike）

非官方的奶牛镇游戏图鉴应用：物品大全 / 配方查询 / NPC 资料 / 装饰预览。基于 Compose Multiplatform，覆盖 Android 与 iOS。

## 免责声明

本应用「使用须知」弹窗、关于页「免责声明」卡片，以及本 README 的文案，与应用内唯一来源 `shared/src/commonMain/kotlin/com/nainiuzhen/wiki/utils/Notice.kt` 的 `NOTICE_TEXT` 逐条一致：

- 本应用为非官方、非营利的爱好者作品，仅供个人学习与技术交流使用。
- 应用内展示的游戏图像、图标、文本及数据均来源于网络，版权归原游戏开发/发行方所有，本应用不主张任何权利。
- 请于下载后 24 小时内自行删除本应用及其中素材；禁止用于任何商业用途，禁止二次打包分发。
- 本应用完全离线运行，不联网、不收集、不上传任何个人信息；您的设置仅保存在本机。
- 如权利人认为本应用侵犯了您的合法权益，请在项目仓库[提交 Issue](https://github.com/Klee7792/nainiuzhenbaike/issues)或通过仓库主页联系开发者，我们将立即停止分发并删除相关内容。
- 继续使用本应用，即表示您已阅读并同意以上全部内容。

> 以上条款的三处展示——首次启动「使用须知」弹窗、关于页「免责声明」卡片、本 README——同源（`shared/src/commonMain/kotlin/com/nainiuzhen/wiki/utils/Notice.kt`）；修改正文后须递增该文件中的 `NOTICE_VERSION` 并同步本 README。
>
> 本应用的唯一对外联系渠道是上述仓库的 Issues —— 应用内不存在其它联系方式。

## 授权范围

- 本仓库的**源代码**以 [Apache-2.0](LICENSE) 协议授权。
- 该授权**不覆盖任何游戏素材**。游戏图像、图标、文本及配置数据均不在本仓库内，存放于私有仓 `nainiuzhen-assets`，仅由 CI 在构建时注入 `assets.pack`；其版权归原游戏开发 / 发行方所有，不得二次分发或商用。

## 目录结构

- `.github/` — CI 工作流（Multi-Platform Build，手动触发）
- `app/` — Android 入口模块（`MainActivity` + `AndroidManifest`，`applicationId = com.nainiuzhen.wiki`）
- `shared/` — Kotlin Multiplatform 共享代码（图鉴逻辑 / 数据 / UI）
- `iosApp/` — iOS 端 Xcode 工程
- `miuix/` — vendored（复合构建）的 miuix（Compose Multiplatform）组件库，Apache-2.0
- `gradle/` — 版本目录（`libs.versions.toml`；仓库无 Gradle Wrapper，CI 直接安装 Gradle 9.6.1）
- `builds/` — 版本号台账（`build_number.txt`、`ios_build_number.txt`）与构建笔记（`build_notes.md`）
- `tools/` — 本地脚本（`pack_assets.py` 打包 `assets.pack`）
  - ⚠️ `tools/build.ps1` **已废弃，请勿运行**：它的 `$RepoRoot` 指向工程的外层目录（并非 git 根）、使用 `git add -A`、会自行 commit 并打 `build-N` tag、硬编码 JDK 17（本工程 `compileOptions` 要求 Java 21），且其 `-replace 'versionCode = \d+'` 已匹配不到 `app/build.gradle.kts` 的现状（该文件版本号现由 `builds/build_number.txt` 决定）。版本号请统一走 `builds/*_build_number.txt` 与 CI，见下节。
- `docs/` — 设计 / 架构文档（PRD、架构、iOS 移植计划等）
- `settings.gradle.kts` / `build.gradle.kts` — Gradle 根配置（含 `includeBuild("miuix")`、`include(":shared")`、`include(":app")`）
- `README.md` / `.gitignore`

## 构建与版本号

- CI 定义在 `.github/workflows/build.yml`，触发方式为 `workflow_dispatch` **手动触发**；push 不会触发出包。
- 版本号来自两份台账，按「百位 . 十位 . 个位」派生版本名：`name = (code/100+1).(code%100/10).(code%10)`：
  - Android 当前 code 存于 `builds/build_number.txt`（当前值 `39` → 版本名 `1.3.9`）；
  - iOS 当前 code 存于 `builds/ios_build_number.txt`（当前值 `10` → 版本名 `1.1.0`）。
- **Android**：`versionCode` / `versionName` 由 `app/build.gradle.kts` 直接读取 `builds/build_number.txt` 决定；CI 的 `version` job 仅把同一台账值通过 `-PncVersionCode` / `-PncVersionName` 属性透传，**不另行计算**。
- **iOS**：由 CI 的 `version` job 读取 `builds/ios_build_number.txt` 后，注入 `xcodebuild` 的 `MARKETING_VERSION` / `CURRENT_PROJECT_VERSION`。
- **发布新版本**：在 Actions 手动触发时勾选 `bump_version`，`version` job 会把两份台账各 +1 并以 `[skip ci]` 提交回推当前分支；不勾则同版本重构建（版本名带 7 位短 sha 区分）。因工作流为 `workflow_dispatch` 手动触发，push 不会自动出包；正常发布流程无需手动改台账——勾选 `bump_version` 后由 CI 完成 +1 与回写。只有不勾 `bump_version`（同版本重构建）时 code 才保持不变。
- **Android 出包**：从私有仓拉取 `assets.pack` 注入 `app/src/main/assets/` 后，`gradle :app:assembleRelease` 产出未签名 release APK（复用 Android debug keystore，仅供内部测试 / 巨魔侧载，非应用商店分发）。
- **iOS 出包**：拉取同一 `assets.pack` 注入 `iosApp/iosApp/`，`xcodebuild` 产出未签名 `.ipa`（`CODE_SIGNING_ALLOWED=NO`），供 TrollStore 侧载。

## 数据来源

- 游戏图像 / 图标 / 文本 / 配置数据来自私有仓 `Klee7792/nainiuzhen-assets`（main 分支根的 `assets.pack`，NZPK v2 容器）。
- 公开仓库**不含任何游戏素材**：CI 在构建时从私有仓拉取并注入 `assets.pack`；缺包则启动为空数据（构建直接失败）。
- 运行时离线读取 `assets.pack`，应用**不联网**（无联网权限、无网络代码）。
