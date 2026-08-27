# 奶牛镇百科 App（nainiuzhen-baike）

基于 **miuix（Compose Multiplatform）** 的安卓游戏图鉴应用：物品大全 / 配方查询 / NPC 资料 / 装饰预览；后续可能扩展其他平台版本。

## 工程状态
- 全新创建中（需求 / 架构设计阶段）。
- 工具链复用 `D:\Android`（SDK / JDK / Gradle 已解压，不入库、不重新解压）。
- 回退机制：Git 仓库在 workspace 根；每次构建由 `tools/build.ps1` 按递增编号存盘 APK 并打 tag。

## 目录约定
- `app/`（或 `android/`）：应用源码，由工程师基于 `miuix/example/android` 复制后编辑
- `builds/`：按编号存盘的 APK（`build_number.txt` 记录当前编号；`build_notes.md` 记录说明）
- `tools/build.ps1`：编号构建脚本
- `docs/`：PRD / 架构设计等文档

## 回退用法
- 查看历史构建：`git tag --list "build-*"` 或 `cat builds/build_notes.md`
- 回到某次构建：`git checkout build-<N>`（源码）＋ 用 `builds/nainiuzhen-baike_v<N>_debug.apk`（安装包）
