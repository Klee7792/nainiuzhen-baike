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

# ---- toolchain (reuse D:\Android, do not re-extract) ----
$env:JAVA_HOME        = "D:\Android\jdk-17.0.20.1"
$env:ANDROID_HOME     = "D:\Android\Sdk"
$env:ANDROID_SDK_ROOT = "D:\Android\Sdk"
# Canonical Gradle cache for this project.
# IMPORTANT (avoid the .lock / ACL poison loop):
#   - Never reuse a GRADLE_USER_HOME that was touched by the Bash *sandbox* — sandbox
#     runs create native-platform.dll.lock / daemon registry.bin.lock owned by a
#     SYSTEM/sandbox token the normal user cannot delete, so every later build dies
#     on AccessDenied. ghome7 is created by the REAL user (non-sandboxed) and only
#     ever holds caches/jdks (never native/daemon poison).
#   - When the agent runs this script it MUST use dangerouslyDisableSandbox so Gradle
#     runs as the real user; otherwise it re-poinsons the cache.
$env:GRADLE_USER_HOME = "D:\ghome7"
$gradle = "D:\Android\gradle-9.6.1\bin\gradle.bat"

# ---- read and bump build number ----
if (-not (Test-Path $NumberFile)) { "0" | Out-File $NumberFile -Encoding ascii }
$buildNo = [int](Get-Content $NumberFile -Raw).Trim()
$buildNo++
Set-Content $NumberFile -Value $buildNo -Encoding ascii -NoNewline

# ---- compile (release + debug dual package) ----
Write-Host "==> start build #$buildNo (release + debug) ..."
& $gradle -p $ProjectRoot clean assembleRelease assembleDebug --no-daemon --stacktrace
if ($LASTEXITCODE -ne 0) { Write-Error "build failed, aborted (build number not rolled back)"; exit 1 }

# ---- locate produced APKs (scope to outputs dir to avoid Gradle .transforms cache noise) ----
$apkOutDir = Join-Path $ProjectRoot "app/build/outputs"
$apks = Get-ChildItem -Path $apkOutDir -Recurse -Filter "*.apk" -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending
function Copy-Apk($variant, $suffix) {
    $apk = $apks | Where-Object { $_.FullName -like "*$variant*" } | Select-Object -First 1
    if (-not $apk) { Write-Error "$variant APK not found under $apkOutDir"; exit 1 }
    $dest = Join-Path $BuildsDir ("nainiuzhen-baike_v{0}_{1}.apk" -f $buildNo, $suffix)
    Copy-Item $apk.FullName $dest -Force
    Write-Host "==> APK stored: $dest"
}
Copy-Apk "release" "release"
Copy-Apk "debug"   "debug"

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
