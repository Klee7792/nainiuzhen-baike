#!/usr/bin/env bash
# v34 构建（复刻 _v33_build.sh：只编译 release；参数按本机内存实测收敛）
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
  --no-daemon --no-parallel --configure-on-demand --max-workers=2 --console=plain \
  "-Dorg.gradle.jvmargs=-Xmx3g -XX:MaxMetaspaceSize=768m -Dfile.encoding=UTF-8" \
  "-Dkotlin.daemon.jvmargs=-Xmx1536m"

# ---- 4. store APK ----
REL=$(find app/build/outputs -name "*release*.apk" | head -1)
if [ -z "$REL" ]; then echo "!! no release apk found" >&2; exit 1; fi
echo "==> release: $REL"
ls -la "$REL"
cp -f "$REL" "builds/nainiuzhen-baike_v${N}_release.apk"

# ---- 5. build notes ----
printf -- "- #%s  %s  v34: iOS 风格悬浮底栏改为按内容宽度收缩并居中(单选项固定 64dp, 底栏宽=选项数x64dp+8dp, 不再撑满主页宽度; 对齐 miuix 悬浮底栏/KernelSU 观感); %s(%s)\n" \
  "$N" "$(date '+%Y-%m-%d %H:%M')" "$VER" "$N" >> builds/build_notes.md
tail -1 builds/build_notes.md

# ---- 6. commit + tag ----
cd /d/1Project/nainiuzhen-wiki
git add nainiuzhen-baike/app/build.gradle.kts \
        nainiuzhen-baike/builds/build_number.txt \
        nainiuzhen-baike/builds/build_notes.md \
        nainiuzhen-baike/shared/src/commonMain/kotlin/com/nainiuzhen/wiki/ui/components/liquid/IosLiquidGlassNavigationBar.kt
git commit -q -m "build #34: iOS 悬浮底栏按内容收缩居中(单选项 64dp 固定宽, 不撑满主页宽度)；$VER($N)"
git tag "build-$N"
echo "==> build #$N done, committed and tagged build-$N"
