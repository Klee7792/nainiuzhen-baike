#!/usr/bin/env bash
# v36 构建：启动进度两阶段真实化 / 冷启动计时 toast / 空态随机文案 / 详情弹窗去背景
set -e
cd /d/1Project/nainiuzhen-wiki/nainiuzhen-baike

# ---- 1. bump build number（35 -> 36）----
N=$(cat builds/build_number.txt | tr -d ' \r\n')
N=$((N + 1))
printf '%s' "$N" > builds/build_number.txt
MAJ=$((N / 10)); MIN=$((N % 10)); VER="1.$MAJ.$MIN"
echo "==> build #$N  version $VER"

# ---- 2. stamp version ----
sed -i "s/versionCode = [0-9]\+/versionCode = $N/" app/build.gradle.kts
sed -i "s/versionName = \"[^\"]*\"/versionName = \"$VER\"/" app/build.gradle.kts
grep -n 'versionCode\|versionName' app/build.gradle.kts

# ---- 3. compile（仅 release；参数为本机 13GB 实测定稿，勿改）----
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
printf -- "- #%s  %s  v36: 启动进度两阶段真实化(准备数据不再卡 0%%, 切片阶段显示帧数) / 冷启动计时 toast(X.XX 秒) / 右栏空态 10 条随机颜文字文案(仅从子页退回才换) / 详情弹窗 6 类块去背景(与手机小屏一致)；%s(%s)\n" \
  "$N" "$(date '+%Y-%m-%d %H:%M')" "$VER" "$N" >> builds/build_notes.md
tail -1 builds/build_notes.md

# ---- 6. commit + tag ----
cd /d/1Project/nainiuzhen-wiki
# 一键纳入本次 v36 全部源码改动（含新增 AppToast/StartupTime 与删除的 Toast.android.kt）；
# **/build/ 与 *.apk 已被 .gitignore 排除，整目录 add 不会带入构建产物。
git add nainiuzhen-baike/app/build.gradle.kts \
        nainiuzhen-baike/builds/build_number.txt \
        nainiuzhen-baike/builds/build_notes.md \
        nainiuzhen-baike/shared
git commit -q -m "build #36: 启动进度两阶段真实化+冷启动计时 toast / 空态随机颜文字文案 / 详情弹窗 6 类块去背景；$VER($N)"
git tag "build-$N"
echo "==> build #$N done, committed and tagged build-$N"
