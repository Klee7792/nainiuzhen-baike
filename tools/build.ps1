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
$gradle = "D:\Android\gradle-9.6.1\bin\gradle.bat"

# ---- read and bump build number ----
if (-not (Test-Path $NumberFile)) { "0" | Out-File $NumberFile -Encoding ascii }
$buildNo = [int](Get-Content $NumberFile -Raw).Trim()
$buildNo++
Set-Content $NumberFile -Value $buildNo -Encoding ascii -NoNewline

# ---- compile ----
Write-Host "==> start build #$buildNo ..."
& $gradle -p $ProjectRoot clean assembleDebug --no-daemon --stacktrace
if ($LASTEXITCODE -ne 0) { Write-Error "build failed, aborted (build number not rolled back)"; exit 1 }

# ---- locate produced APK (scope to outputs dir to avoid Gradle .transforms cache noise) ----
$apkOutDir = Join-Path $ProjectRoot "app/build/outputs"
$apk = Get-ChildItem -Path $apkOutDir -Recurse -Filter "*.apk" -ErrorAction SilentlyContinue |
       Where-Object { $_.FullName -like "*debug*" } |
       Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $apk) { Write-Error "debug APK not found under $apkOutDir"; exit 1 }

$dest = Join-Path $BuildsDir ("nainiuzhen-baike_v{0}_debug.apk" -f $buildNo)
Copy-Item $apk.FullName $dest -Force
Write-Host "==> APK stored: $dest"

# ---- build notes ----
if (-not (Test-Path $NoteFile)) { "# Build Notes`n" | Out-File $NoteFile -Encoding utf8 }
$stamp = Get-Date -Format "yyyy-MM-dd HH:mm"
Add-Content $NoteFile ("- #$buildNo  $stamp  $Message")

# ---- git commit + tag (commit code state at repo root for easy rollback) ----
# Robust git invocation: run git via System.Diagnostics.Process with stderr/stdout
# redirected to the process object. This keeps git's stderr (e.g. CRLF warnings)
# OUT of PowerShell's error stream, so ErrorActionPreference=Stop never turns a
# harmless warning into a fatal NativeCommandError. Real failures are detected
# via the process ExitCode. (APK binaries stay on disk, excluded by .gitignore;
# the tag marks the code state for rollback.)
function Invoke-GitRobust([string]$GitArgs) {
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = "git"
    $psi.Arguments = $GitArgs
    $psi.WorkingDirectory = $RepoRoot
    $psi.UseShellExecute = $false
    $psi.RedirectStandardError = $true
    $psi.RedirectStandardOutput = $true
    $p = New-Object System.Diagnostics.Process
    $p.StartInfo = $psi
    $null = $p.Start()
    $p.WaitForExit()
    if ($p.ExitCode -ne 0) {
        $err = $p.StandardError.ReadToEnd()
        Write-Host "git error (args: $GitArgs): $err"
    }
    return $p.ExitCode
}

$rc = Invoke-GitRobust "add -A"
if ($rc -ne 0) { Write-Error "git add failed"; exit 1 }
$rc = Invoke-GitRobust ("commit -q -m ""build #$buildNo $Message""")
if ($rc -ne 0) { Write-Error "git commit failed"; exit 1 }
$rc = Invoke-GitRobust ("tag build-" + $buildNo)
if ($rc -ne 0) { Write-Error "git tag failed"; exit 1 }

Write-Host "==> build #$buildNo done, committed and tagged build-$buildNo"
