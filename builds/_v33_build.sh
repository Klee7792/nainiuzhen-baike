#!/usr/bin/env bash
# v33 构建（复刻 _v32_build.sh：只编译 release，debug 按需另出）
set -e
cd /d/1Project/nainiuzhen-wiki/nainiuzhen-baike

# ---- 1. bump build number ----
N=$(cat builds/build_number.txt | tr -d ' \r\n')
N=$((N + 1))
printf '%s' "$N" > builds/build_number.txt
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
  --no-daemon --no-parallel --configure-on-demand --max-workers=3 --console=plain \
  "-Dorg.gradle.jvmargs=-Xmx3g -XX:MaxMetaspaceSize=768m -Dfile.encoding=UTF-8" \
  "-Dkotlin.daemon.jvmargs=-Xmx2g"

# ---- 4. store APK（仅 release；缺文件即失败）----
REL=$(find app/build/outputs -name "*release*.apk" | head -1)
if [ -z "$REL" ]; then echo "!! no release apk found" >&2; exit 1; fi
echo "==> release: $REL"
ls -la "$REL"
cp -f "$REL" "builds/nainiuzhen-baike_v${N}_release.apk"

# ---- 5. build notes ----
printf -- "- #%s  %s  v33: 启动预热切片全量进内存(不再写cacheDir, 滚动零IO零解码) + 加载页真实0-100%%切片进度 + 加载页图标换成assets/ic_launcher.png + 隐藏设置页\"清理缓存\"入口; 卡片文字: 物品/配方新增0.5x-1.0x倍率滑块(0.5-1.0, 1.0=当前字号11sp为最大, 步长跟随页面步长设置); 卡片名称胶囊超宽时自动跑马灯(静止3s->35dp/s恒速滚一遍->停2s->再滚一遍->回位循环, 手指拖动立即打断); %s(%s)\n" \
  "$N" "$(date '+%Y-%m-%d %H:%M')" "$VER" "$N" >> builds/build_notes.md
tail -2 builds/build_notes.md

# ---- 6. commit + tag ----
cd /d/1Project/nainiuzhen-wiki
git add nainiuzhen-baike/app/build.gradle.kts \
        nainiuzhen-baike/builds/build_number.txt \
        nainiuzhen-baike/builds/build_notes.md \
        nainiuzhen-baike/shared/src/commonMain/kotlin/com/nainiuzhen/wiki/App.kt \
        nainiuzhen-baike/shared/src/commonMain/kotlin/com/nainiuzhen/wiki/data/repository/SpriteRepository.kt \
        nainiuzhen-baike/shared/src/commonMain/kotlin/com/nainiuzhen/wiki/data/source/SpriteSlicer.kt \
        nainiuzhen-baike/shared/src/commonMain/kotlin/com/nainiuzhen/wiki/ui/components/SectionCardParts.kt \
        nainiuzhen-baike/shared/src/commonMain/kotlin/com/nainiuzhen/wiki/ui/items/ItemListScreen.kt \
        nainiuzhen-baike/shared/src/commonMain/kotlin/com/nainiuzhen/wiki/ui/recipe/RecipeListScreen.kt \
        nainiuzhen-baike/shared/src/commonMain/kotlin/com/nainiuzhen/wiki/ui/settings/ImageScaleSettingsScreen.kt \
        nainiuzhen-baike/shared/src/commonMain/kotlin/com/nainiuzhen/wiki/ui/settings/SettingsScreen.kt \
        nainiuzhen-baike/shared/src/commonMain/kotlin/com/nainiuzhen/wiki/utils/AppState.kt \
        nainiuzhen-baike/shared/src/androidMain/kotlin/com/nainiuzhen/wiki/platform/AndroidAppSettingsStore.kt \
        nainiuzhen-baike/shared/src/androidMain/kotlin/com/nainiuzhen/wiki/platform/AndroidSpriteCacheManager.kt \
        nainiuzhen-baike/shared/src/androidMain/kotlin/com/nainiuzhen/wiki/platform/AndroidSpriteSlicer.kt
git commit -q -m "build #33: v33 —— 启动预热切片进内存+真实0-100%进度+加载页真图标 / 卡片文字倍率滑块 / 名称胶囊跑马灯；$VER($N)"
git tag "build-$N"
echo "==> build #$N done, committed and tagged build-$N"
