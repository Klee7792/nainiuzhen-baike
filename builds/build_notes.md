# Build Notes

- #1  2026-08-28 00:53  first build: 3 sections UI + MainActivity, Gradle 9.6.1, robust APK locate
- #1  2026-08-28 00:56  first build: 3 sections UI + MainActivity, Gradle 9.6.1, robust APK locate + git
- #2  2026-08-28 01:22  fix: app crashes on launch - compose resources not packaged into APK; switch asset loading to native Android assets/ (AssetLoader expect/actual + AppContextHolder)
- #3  2026-08-28 02:04  修复v2 JSON null崩溃、v3 ANR；assets重组为res/config
