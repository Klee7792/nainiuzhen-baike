#!/usr/bin/env bash
# v34 重跑（第三次 —— 前两次都在 :app:mergeDexRelease 原生 OOM 崩，hs_err 显示 malloc/Chunk::new 失败。
# 本机 13GB 但系统已启用内存压缩，可用余量小。策略：降 daemon 堆到 1536m 给 dex worker 留原生内存、
# max-workers=1、跳过 lintVital（它和 dex 同阶段抢内存）。复用已 bump 的 34，不跳号。）
set -e
cd /d/1Project/nainiuzhen-wiki/nainiuzhen-baike

# ---- 1. 复用现有 build number（34）----
N=$(cat builds/build_number.txt | tr -d ' \r\n')
MAJ=$((N / 10)); MIN=$((N % 10)); VER="1.$MAJ.$MIN"
echo "==> build #$N  version $VER"

# ---- 2. stamp version（幂等）----
sed -i "s/versionCode = [0-9]\+/versionCode = $N/" app/build.gradle.kts
sed -i "s/versionName = \"[^\"]*\"/versionName = \"$VER\"/" app/build.gradle.kts
grep -n 'versionCode\|versionName' app/build.gradle.kts

# ---- 3. compile（仅 release，跳过 lintVital）----
JAVA_HOME="D:\Users\Yun\Documents\1Windows\jdk-21.0.12.1" \
GRADLE_USER_HOME="D:\Users\Yun\Documents\1Windows\Android\gradle-home" \
MSYS_NO_PATHCONV=1 \
"D:\Users\Yun\Documents\1Windows\Android\gradle-9.6.1\bin\gradle.bat" \
  clean assembleRelease \
  --no-daemon --no-parallel --configure-on-demand --max-workers=1 --console=plain \
  "-Dorg.gradle.jvmargs=-Xmx1536m -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8" \
  "-Dkotlin.daemon.jvmargs=-Xmx1280m"

# ---- 4. store APK ----
REL=$(find app/build/outputs -name "*release*.apk" | head -1)
if [ -z "$REL" ]; then echo "!! no release apk found" >&2; exit 1; fi
echo "==> release: $REL"
ls -la "$REL"
cp -f "$REL" "builds/nainiuzhen-baike_v${N}_release.apk"

# ---- 5. build notes ----
printf -- "- #%s  %s  v34: iOS 悬浮底栏按内容收缩居中(单选项 64dp 固定宽, 不再撑满主页宽度) / 卡片文字改为绝对值 5~11sp(默认 8, 步长跟随 0.1/0.5/1) / AppState.kt 由 GBK 转 UTF-8；%s(%s)\n" \
  "$N" "$(date '+%Y-%m-%d %H:%M')" "$VER" "$N" >> builds/build_notes.md
tail -1 builds/build_notes.md

# ---- 6. commit + tag ----
cd /d/1Project/nainiuzhen-wiki
git add nainiuzhen-baike/app/build.gradle.kts \
        nainiuzhen-baike/builds/build_number.txt \
        nainiuzhen-baike/builds/build_notes.md \
        nainiuzhen-baike/shared/src/commonMain/kotlin/com/nainiuzhen/wiki/ui/components/liquid/IosLiquidGlassNavigationBar.kt \
        nainiuzhen-baike/shared/src/commonMain/kotlin/com/nainiuzhen/wiki/utils/AppState.kt \
        nainiuzhen-baike/shared/src/androidMain/kotlin/com/nainiuzhen/wiki/platform/AndroidAppSettingsStore.kt \
        nainiuzhen-baike/shared/src/commonMain/kotlin/com/nainiuzhen/wiki/ui/settings/ImageScaleSettingsScreen.kt \
        nainiuzhen-baike/shared/src/commonMain/kotlin/com/nainiuzhen/wiki/ui/items/ItemListScreen.kt \
        nainiuzhen-baike/shared/src/commonMain/kotlin/com/nainiuzhen/wiki/ui/recipe/RecipeListScreen.kt
git commit -q -m "build #34: iOS 悬浮底栏按内容收缩居中 / 卡片文字改绝对值 5~11sp(默认8) / AppState.kt 转 UTF-8；$VER($N)"
git tag "build-$N"
echo "==> build #$N done, committed and tagged build-$N"
