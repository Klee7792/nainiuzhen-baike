#!/usr/bin/env bash
# v35 重跑（第一次在 :shared:compileAndroidMain 编译失败：WindowInsets.systemBars 在 commonMain 不存在，
# 已改成 statusBars / navigationBars。build_number 已 bump 到 35，本次复用不再 +1。）
set -e
cd /d/1Project/nainiuzhen-wiki/nainiuzhen-baike

# ---- 1. 复用现有 build number（35）----
N=$(cat builds/build_number.txt | tr -d ' \r\n')
MAJ=$((N / 10)); MIN=$((N % 10)); VER="1.$MAJ.$MIN"
echo "==> build #$N  version $VER"

# ---- 2. stamp version ----
sed -i "s/versionCode = [0-9]\+/versionCode = $N/" app/build.gradle.kts
sed -i "s/versionName = \"[^\"]*\"/versionName = \"$VER\"/" app/build.gradle.kts
grep -n 'versionCode\|versionName' app/build.gradle.kts

# ---- 3. compile（仅 release）----
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
printf -- "- #%s  %s  v35: 卡片名称跑马灯改首尾相连一圈(短名补空格到本页最长宽度, 全页行程/速度/耗时一致; 只在状态栏~导航栏可视区内滚; 每轮只滚 2 圈后停止) / 卡片文字默认字号 8→10sp；%s(%s)\n" \
  "$N" "$(date '+%Y-%m-%d %H:%M')" "$VER" "$N" >> builds/build_notes.md
tail -1 builds/build_notes.md

# ---- 6. commit + tag ----
cd /d/1Project/nainiuzhen-wiki
git add nainiuzhen-baike/app/build.gradle.kts \
        nainiuzhen-baike/builds/build_number.txt \
        nainiuzhen-baike/builds/build_notes.md \
        nainiuzhen-baike/shared/src/commonMain/kotlin/com/nainiuzhen/wiki/ui/components/CardMarquee.kt \
        nainiuzhen-baike/shared/src/commonMain/kotlin/com/nainiuzhen/wiki/ui/components/SectionCardParts.kt \
        nainiuzhen-baike/shared/src/commonMain/kotlin/com/nainiuzhen/wiki/utils/AppState.kt \
        nainiuzhen-baike/shared/src/androidMain/kotlin/com/nainiuzhen/wiki/platform/AndroidAppSettingsStore.kt \
        nainiuzhen-baike/shared/src/commonMain/kotlin/com/nainiuzhen/wiki/ui/settings/ImageScaleSettingsScreen.kt \
        nainiuzhen-baike/shared/src/commonMain/kotlin/com/nainiuzhen/wiki/ui/items/ItemListScreen.kt \
        nainiuzhen-baike/shared/src/commonMain/kotlin/com/nainiuzhen/wiki/ui/recipe/RecipeListScreen.kt \
        nainiuzhen-baike/shared/src/commonMain/kotlin/com/nainiuzhen/wiki/ui/npc/NpcListScreen.kt
git commit -q -m "build #35: 卡片名称跑马灯改首尾相连一圈+同页同步(只滚2圈/仅屏幕区) / 卡片文字默认 10sp；$VER($N)"
git tag "build-$N"
echo "==> build #$N done, committed and tagged build-$N"
