# build.ps1 — 奶牛镇百科 App 编号构建脚本
# 用法:  .\tools\build.ps1 "本次构建说明"
# 作用:  递增构建号 -> 编译 debug APK -> 带编号存盘 -> 提交并打 git tag
# 回退:  每个构建号对应一个 APK 文件 + 一个 git tag(build-N) + 一次提交
param(
    [string]$Message = ""
)

$ErrorActionPreference = "Stop"

# ---- 路径 ----
$ProjectRoot = Split-Path -Parent $PSScriptRoot                      # nainiuzhen-baike/
$RepoRoot    = Split-Path -Parent $ProjectRoot                      # nainiuzhen-wiki/ (git 仓库根)
$BuildsDir   = Join-Path $ProjectRoot "builds"
$NumberFile  = Join-Path $BuildsDir "build_number.txt"
$NoteFile    = Join-Path $BuildsDir "build_notes.md"

# ---- 工具链（复用 D:\Android 已解压环境，不重复解压） ----
$env:JAVA_HOME        = "D:\Android\jdk-17.0.20.1"
$env:ANDROID_HOME     = "D:\Android\Sdk"
$env:ANDROID_SDK_ROOT = "D:\Android\Sdk"
$gradle = "D:\Android\gradle-9.6.1\bin\gradle.bat"

# ---- 读取并递增构建号 ----
if (-not (Test-Path $NumberFile)) { "0" | Out-File $NumberFile -Encoding ascii }
$buildNo = [int](Get-Content $NumberFile -Raw).Trim()
$buildNo++
Set-Content $NumberFile -Value $buildNo -Encoding ascii -NoNewline

# ---- 编译 ----
Write-Host "==> 开始构建 #$buildNo ..."
& $gradle -p $ProjectRoot clean assembleDebug --no-daemon --stacktrace
if ($LASTEXITCODE -ne 0) { Write-Error "构建失败，已中止（构建号未回写）"; exit 1 }

# ---- 找出产物 APK ----
$apk = Get-ChildItem -Path $ProjectRoot -Recurse -Filter "*.apk" |
       Where-Object { $_.FullName -like "*debug*" } |
       Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $apk) { Write-Error "未找到 debug APK"; exit 1 }

$dest = Join-Path $BuildsDir ("nainiuzhen-baike_v{0}_debug.apk" -f $buildNo)
Copy-Item $apk.FullName $dest -Force
Write-Host "==> APK 已存盘: $dest"

# ---- 构建笔记 ----
if (-not (Test-Path $NoteFile)) { "# 构建记录`n" | Out-File $NoteFile -Encoding utf8 }
$stamp = Get-Date -Format "yyyy-MM-dd HH:mm"
Add-Content $NoteFile ("- #$buildNo  $stamp  $Message")

# ---- Git 提交 + 打 tag（在仓库根提交完整状态，便于回退） ----
git -C $RepoRoot add -A
git -C $RepoRoot commit -q -m "build #$buildNo $Message"
git -C $RepoRoot tag ("build-" + $buildNo)

Write-Host "==> 已完成构建 #$buildNo，已提交并打 tag build-$buildNo"
