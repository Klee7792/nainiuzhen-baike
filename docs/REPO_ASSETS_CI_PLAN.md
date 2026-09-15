# GitHub 仓库 / CI / 素材保护 计划

> 2026-09-15 讨论定稿。执行前先读本文件；每完成一项打勾。

## 一、仓库结构（已定）

- **主仓库（公开）**：https://github.com/Klee7792/nainiuzhen-baike.git
- 仓库根 = `nainiuzhen-baike/` 的内容；外围目录（`nainiuzhen/` 游戏解包、`KernelSU/` 参考、`res/`、`tool/`、各种 txt/log/zip）**一律不进仓库**。
- **miuix vendor 进主仓库**：`miuix/` 移入 `nainiuzhen-baike/miuix/`，删除其 `.git`（嵌套 git 仓库不会被外层跟踪），清掉 `build/`、`.gradle/`、example 构建产物、两个 web zip；`settings.gradle.kts` 改相对路径 `includeBuild("miuix")`（摆脱 `D:/1Project` 绝对路径，任何机器可编）。
- 唯一进仓库的图片：`app/src/main/assets/ic_launcher.png`（自绘图标）。
- **游戏素材一个字节都不进主仓库**——走私有素材仓库（见三）。

## 二、工具链对齐（换 miuix 1206 新库引发）

| 项 | 旧 | 新 | 状态 |
|---|---|---|---|
| AGP | 9.3.2 | **9.4.0**（miuix 1206 要求，复合构建不允许双版本共存） | wiki `gradle/libs.versions.toml` 已改 |
| Gradle | 9.6.1 | 9.6.1 先试；若被 AGP 9.4 拒则升 **9.7.1**（miuix wrapper 同款；curl 下载被环境拦截时，用 miuix 自带 `gradlew` 引导下载到 GRADLE_USER_HOME） | 验证中 |
| Kotlin | 2.4.10 | miuix 用 2.4.20，同 minor 元数据兼容，**暂不升** | — |
| 坑 | miuix 仓库 publication 插件配置期跑 git | 已验证新仓库 git 可用（hash `1176c4fa`） | ✅ |

- miuix 1206 结构变化备忘：不再有 `miuix`/`miuix-extension` 聚合模块；`miuix-icon`→`miuix-icons`；新增 `miuix-core`/`miuix-shader`/`miuix-nav`；坐标仍是 `top.yukonga.miuix.kmp:miuix-*:0.9.4`（1.0.9/1206 是 demo 应用版本号）。

## 三、素材保护：assets.pack 容器（防小白定位）

**打包内容**（本地 Python 脚本生成，参考 `smart_picture_tool.py` 的魔数知识）：
- 图片：魔数破坏（原始素材天然如此）+ 哈希改名打散 + XOR（固定 key 编译进代码）；
- JSON（item_database 等）：XOR + base64；
- 全部装进**单一 blob + 偏移索引** = `assets.pack`。

**启动管线**（改造 AssetManager，插进现有 0-100% 切片进度回调，不破坏现有架构）：
读 pack → 解 XOR → 内存中修魔数 → 切片 → 内存缓存（现有链路）。

**定位共识**：防小白/防君子。APK 人人可解包，客户端保护天花板就是"提高门槛"；单一容器让解包者连独立图片都看不到，比散装魔数破坏强一档。AES 不做（key 仍在 APK，收益低）。

## 四、CI：公开主仓库 + 私有素材仓库（定稿方案）

- **私有素材仓库**（新建，如 `nainiuzhen-assets`）：`assets.pack` 作为 **Release 附件**发布，tag 版本化；素材更新 = 发新 Release。
- **主仓库 workflow 拉取**：主仓库 Secrets 存 **fine-grained PAT**（仅授权素材仓库、仅 `contents:read`），构建前 `curl -H "Authorization: Bearer ${{ secrets.* }}"` 下载 pack 到 `assets/`。
- **白嫖不受影响**：计费只看跑 workflow 的仓库（主仓库公开 = macOS 无限免费）；从私有仓库下载不产生费用。
- **好处**：Android CI 直接出**完整**包（无需本地打包+注入）；素材从未公开，公开历史干净，转私无负担。

### Android 产物
- `assembleRelease` 完整 APK → workflow artifact（可选 Release）。
- ⚠️ **签名待拍板**：A. keystore base64 存 Secrets、构建时解码（正式）；B. 提交一个专用 keystore 进仓库（个人项目常用，简单）。
- 公开仓库 CI 出的"无素材空壳包"问题随拉取方案自动消失。

### iOS 产物（巨魔路线，无 $99 账号）
- macos runner + `CODE_SIGNING_ALLOWED=NO` 构建 → `.app` 打包成 `Payload/xxx.app` → zip 改 `.ipa` → artifact → **TrollStore 安装**（支持 iOS 14.0–16.6.1 / 17.0）。
- CMP iOS 工作量（独立里程碑，非重写）：补 `iosMain` 的 expect/actual（设置存储 NSUserDefaults、**素材管线最重**：pack 读取走 iOS bundle、切片预热适配）、iOS 壳工程（Xcode + CMP framework 接线）、workflow。
- 建议第一步：CI 出模拟器包验证 UI，再上真机（巨魔）。

## 五、公开 ↔ 私有序列

1. 公开期：主仓库只有代码 + miuix + ic_launcher；素材走私有仓库拉取（**不用等转私**）。
2. iOS 稳定后：Settings → General → Danger Zone → Change visibility → Make private（无损，历史/ tags/ Releases 全保留）。
3. 转私后 macOS 按 10 倍计费（免费额度 2000 分/月 ≈ 200 macOS 分钟）。
4. 注意：公开期间推过的内容永远"曾经公开过"——素材从头不进主仓库，故无暴露。

## 六、待办清单

- [ ] miuix vendor 进 `nainiuzhen-baike/`（去 .git/build）+ `includeBuild("miuix")` 相对路径
- [ ] 转场修复收尾（本轮编译验证：AOSP=CrossActivityTransition / 滑动返回显式方向 / 压暗+阻止输入补 UI；版本保持 37，等用户指示再 tag 进 38）
- [ ] `assets.pack` 打包脚本（Python，魔数破坏+改名+XOR+容器索引）
- [ ] AssetManager 改造：读 pack → 解 XOR → 修魔数 → 切片（进度回调兼容）
- [ ] keystore 签名方案拍板（Secrets 解码 vs 仓库内专用 keystore）
- [ ] workflow：`android.yml`（拉 pack → assembleRelease → artifact）、`ios.yml`（未签名 ipa）
- [ ] （过渡备选）本地注入脚本：APK zip 注入 + zipalign + apksigner——CI 拉取方案落地后可省
- [ ] iOS 里程碑：iosMain actuals + 壳工程 + 模拟器验证

## 七、背景知识备忘

- Git 提交哈希 = SHA-1（40 位十六进制，显示前 7 位），非 CRC32。
- 巨魔（TrollStore）可装免签名 ipa，绕过 Apple 开发者账号。
- GitHub 公开仓库 Actions 免费（含 macOS）；私有仓库 macOS 按 10 倍计费。
- `smart_picture_tool.py` = 修复游戏图片魔数的脚本；`resource/` = 未修复（魔数破坏）的原始素材，天然第一层混淆。
