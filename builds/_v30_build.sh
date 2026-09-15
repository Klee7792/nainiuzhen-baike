#!/usr/bin/env bash
# v30 发布构建（手动等价于 tools/build.ps1，但补上内存安全参数）
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

# ---- 3. compile ----
JAVA_HOME="D:\Users\Yun\Documents\1Windows\jdk-21.0.12.1" \
GRADLE_USER_HOME="D:\Users\Yun\Documents\1Windows\Android\gradle-home" \
MSYS_NO_PATHCONV=1 \
"D:\Users\Yun\Documents\1Windows\Android\gradle-9.6.1\bin\gradle.bat" \
  clean assembleRelease assembleDebug \
  --no-daemon --no-parallel --configure-on-demand --max-workers=3 --console=plain \
  "-Dorg.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=768m -Dfile.encoding=UTF-8" \
  "-Dkotlin.daemon.jvmargs=-Xmx2g"

# ---- 4. store APK ----
REL=$(find app/build/outputs -name "*release*.apk" | head -1)
DBG=$(find app/build/outputs -name "*debug*.apk" | head -1)
echo "==> release: $REL"; echo "==> debug:   $DBG"
ls -la "$REL" "$DBG"
cp -f "$REL" "builds/nainiuzhen-baike_v${N}_release.apk"
cp -f "$DBG" "builds/nainiuzhen-baike_v${N}_debug.apk"

# ---- 5. build notes ----
printf -- "- #%s  %s  大屏适配收尾: 主页-子页分栏 + 左栏平级跳转(openTopLevel) + 弹窗左栏补遮罩(弹窗优先) + 弹窗右栏自动居中(零偏移常量); 文档 LARGE_SCREEN_ADAPTATION.md 已同步; %s(%s)\n" \
  "$N" "$(date '+%Y-%m-%d %H:%M')" "$VER" "$N" >> builds/build_notes.md
tail -2 builds/build_notes.md

# ---- 6. commit + tag（精确暂存，不用 git add -A，避免混入日志/临时目录）----
cd /d/1Project/nainiuzhen-wiki
git add nainiuzhen-baike/builds/build_number.txt \
        nainiuzhen-baike/builds/build_notes.md \
        nainiuzhen-baike/app/build.gradle.kts
git commit -q -m "build #$N: 大屏适配收尾 —— 主页-子页分栏 / openTopLevel / 弹窗左栏补遮罩；$VER($N)"
git tag "build-$N"
echo "==> build #$N done, committed and tagged build-$N"
