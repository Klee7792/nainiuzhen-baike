# Build Notes

- #1  2026-08-28 00:53  first build: 3 sections UI + MainActivity, Gradle 9.6.1, robust APK locate
- #1  2026-08-28 00:56  first build: 3 sections UI + MainActivity, Gradle 9.6.1, robust APK locate + git
- #2  2026-08-28 01:22  fix: app crashes on launch - compose resources not packaged into APK; switch asset loading to native Android assets/ (AssetLoader expect/actual + AppContextHolder)
- #3  2026-08-28 02:04  修复v2 JSON null崩溃、v3 ANR；assets重组为res/config
- #4  2026-08-28 02:52  fix: crash(root cause Infinity-scroll)+CCW sprite rotation+settings persistence+bottom-bar 2 tabs+PRD item/recipe dialogs+NPC 3-col grid+6-block detail+schedule married/unmarried filter+timeline
- #5  2026-08-28 14:39  v5: 色彩模式6态+Monet开关+14开关+关于页; 主页3白卡; 物品6列方形; 星价卡片; 配方/NPC弹窗改版; 切后台重载修复(cachedLoadedData); 包名com.nainiuzhen.wiki
