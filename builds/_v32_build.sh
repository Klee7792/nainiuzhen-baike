#!/usr/bin/env bash
# v32 修复构建（复刻 _v31_build.sh；新约定：只编译 release，debug 按需另出）
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
  "-Dorg.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=768m -Dfile.encoding=UTF-8" \
  "-Dkotlin.daemon.jvmargs=-Xmx2g"

# ---- 4. store APK（仅 release；缺文件即失败）----
REL=$(find app/build/outputs -name "*release*.apk" | head -1)
if [ -z "$REL" ]; then echo "!! no release apk found" >&2; exit 1; fi
echo "==> release: $REL"
ls -la "$REL"
cp -f "$REL" "builds/nainiuzhen-baike_v${N}_release.apk"

# ---- 5. build notes ----
printf -- "- #%s  %s  v32 修复包: [Bug1]横屏打开子页时\"暂无内容\"层随MiuixDefault转场-0.25x宽度视差左移溢出详情槽、盖住左侧栏成黑块 -> 详情层Box加clipToBounds裁剪; [Bug2]卡片按下阴影的常驻clip(卡片形状)把名称胶囊下半永久削掉 -> 移除持久clip, 改为drawWithContent里仅按住时按卡片形状绘制黑10%%覆盖(松手即消失), 对齐MiuixIndication的PRESS_ALPHA_DELTA=0.10; 新约定: 只编译release; %s(%s)\n" \
  "$N" "$(date '+%Y-%m-%d %H:%M')" "$VER" "$N" >> builds/build_notes.md
tail -2 builds/build_notes.md

# ---- 6. commit + tag（源码修复一并纳入）----
cd /d/1Project/nainiuzhen-wiki
git add nainiuzhen-baike/builds/build_number.txt \
        nainiuzhen-baike/builds/build_notes.md \
        nainiuzhen-baike/app/build.gradle.kts \
        nainiuzhen-baike/shared/src/commonMain/kotlin/com/nainiuzhen/wiki/ui/home/MainScreen.kt \
        nainiuzhen-baike/shared/src/commonMain/kotlin/com/nainiuzhen/wiki/ui/components/SectionCardParts.kt
git commit -q -m "build #32: v32 修复包 —— 横屏子页视差黑块(详情槽clipToBounds) / 按下阴影改瞬态反馈不再裁胶囊；$VER($N)"
git tag "build-$N"
echo "==> build #$N done, committed and tagged build-$N"
