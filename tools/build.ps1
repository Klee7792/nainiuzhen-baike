# build.ps1 - Nainiuzhen Encyclopedia App numbered build script
# Usage:  .\tools\build.ps1 "build note"
# Effect: bump build number -> compile debug APK -> store numbered APK -> commit + git tag
# Rollback: each build number maps to one APK file + one git tag (build-N) + one commit
param(
    [string]$Message = ""
)

$ErrorActionPreference = "Stop"

# ---- paths ----
$ProjectRoot = Split-Path -Parent $PSScriptRoot                      # nainiuzhen-baike/
$RepoRoot    = Split-Path -Parent $ProjectRoot                      # nainiuzhen-wiki/ (git repo root)
$BuildsDir   = Join-Path $ProjectRoot "builds"
$NumberFile  = Join-Path $BuildsDir "build_number.txt"
$NoteFile    = Join-Path $BuildsDir "build_notes.md"

# ---- toolchain (portable, under D:\Users\Yun\Documents\1Windows) ----
$env:JAVA_HOME        = "D:\Users\Yun\Documents\1Windows\jdk-17.0.20.1"
$env:ANDROID_HOME     = "D:\Users\Yun\Documents\1Windows\Android\Sdk"
$env:ANDROID_SDK_ROOT = "D:\Users\Yun\Documents\1Windows\Android\Sdk"
# Canonical Gradle cache for this project.
# IMPORTANT (avoid the .lock / ACL poison loop):
#   - Never reuse a GRADLE_USER_HOME that was touched by the Bash *sandbox* — sandbox
#     runs create native-platform.dll.lock / daemon registry.bin.lock owned by a
#     SYSTEM/sandbox token the normal user cannot delete, so every later build dies
#     on AccessDenied. The portable gradle-home is created by the REAL user
#     (non-sandboxed) and only ever holds caches/jdks (never native/daemon poison).
#   - When the agent runs this script it MUST use dangerouslyDisableSandbox so Gradle
#     runs as the real user; otherwise it re-poinsons the cache.
#   - ghome7 (gr-home) is the older cache on D:\ghome7; kept only for reference.
$env:GRADLE_USER_HOME = "D:\Users\Yun\Documents\1Windows\Android\gradle-home"
$gradle = "D:\Users\Yun\Documents\1Windows\Android\gradle-9.6.1\bin\gradle.bat"

# ---- read and bump build number ----
if (-not (Test-Path $NumberFile)) { "0" | Out-File $NumberFile -Encoding ascii }
$buildNo = [int](Get-Content $NumberFile -Raw).Trim()
$buildNo++
Set-Content $NumberFile -Value $buildNo -Encoding ascii -NoNewline

# ---- stamp version from build number (single source of truth = build_number.txt) ----
# 应用内展示版本（APP_VERSION_NAME）与 Android 应用信息（versionName/versionCode）随 build 自动 +1，
# 无需手动改多处。版本名约定「遇 10 进 1」：build N -> 1.(N/10).(N%10)
#   例：build 9 -> 1.0.9，build 13 -> 1.1.3，build 20 -> 1.2.0
$major = [math]::Floor($buildNo / 10)
$minor = $buildNo % 10
$verName = "1.$major.$minor"
$verDisp = "v$verName"
$gradleKts  = Join-Path $ProjectRoot "app/build.gradle.kts"
(Get-Content $gradleKts)  -replace 'versionCode = \d+',              "versionCode = $buildNo"           | Set-Content $gradleKts
(Get-Content $gradleKts)  -replace 'versionName = "[^"]*"',          "versionName = `"$verName`""       | Set-Content $gradleKts
Write-Host "==> version stamped: $verDisp (versionCode $buildNo)"

# ---- compile (release only; debug 不再产出) ----
Write-Host "==> start build #$buildNo (release) ..."
& $gradle -p $ProjectRoot clean assembleRelease --no-daemon --stacktrace
if ($LASTEXITCODE -ne 0) { Write-Error "build failed, aborted (build number not rolled back)"; exit 1 }

# ---- locate produced APK (scope to outputs dir to avoid Gradle .transforms cache noise) ----
$apkOutDir = Join-Path $ProjectRoot "app/build/outputs"
$apks = Get-ChildItem -Path $apkOutDir -Recurse -Filter "*.apk" -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending
$apk = $apks | Where-Object { $_.FullName -like "*release*" } | Select-Object -First 1
if (-not $apk) { Write-Error "release APK not found under $apkOutDir"; exit 1 }
$dest = Join-Path $BuildsDir ("nainiuzhen-baike_v{0}_release.apk" -f $buildNo)
Copy-Item $apk.FullName $dest -Force
Write-Host "==> APK stored: $dest"

# ---- build notes ----
if (-not (Test-Path $NoteFile)) { "# Build Notes`n" | Out-File $NoteFile -Encoding utf8 }
$stamp = Get-Date -Format "yyyy-MM-dd HH:mm"
Add-Content $NoteFile ("- #$buildNo  $stamp  $Message")

# ---- git commit + tag (commit code state at repo root for easy rollback) ----
# Use direct git calls (no System.Diagnostics.Process redirect, which can deadlock
# when git's stderr buffer fills). Scope ErrorActionPreference to Continue so git's
# harmless stderr (e.g. CRLF warnings) is not treated as a fatal error, discard
# stderr with 2>$null, and detect real failures via $LASTEXITCODE.
# (APK binaries stay on disk, excluded by .gitignore; the tag marks the code state
# for rollback.)
$prevEap = $ErrorActionPreference
$ErrorActionPreference = "Continue"
try {
    git -C $RepoRoot add -A 2>$null
    if ($LASTEXITCODE -ne 0) { Write-Error "git add failed"; exit 1 }
    git -C $RepoRoot commit -q -m "build #$buildNo $Message" 2>$null
    if ($LASTEXITCODE -ne 0) { Write-Error "git commit failed"; exit 1 }
    git -C $RepoRoot tag ("build-" + $buildNo) 2>$null
    if ($LASTEXITCODE -ne 0) { Write-Error "git tag failed"; exit 1 }
} finally {
    $ErrorActionPreference = $prevEap
}

Write-Host "==> build #$buildNo done, committed and tagged build-$buildNo"
