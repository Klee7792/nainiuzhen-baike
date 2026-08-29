# Build Notes

- #1  2026-08-28 00:53  first build: 3 sections UI + MainActivity, Gradle 9.6.1, robust APK locate
- #1  2026-08-28 00:56  first build: 3 sections UI + MainActivity, Gradle 9.6.1, robust APK locate + git
- #2  2026-08-28 01:22  fix: app crashes on launch - compose resources not packaged into APK; switch asset loading to native Android assets/ (AssetLoader expect/actual + AppContextHolder)
- #3  2026-08-28 02:04  修复v2 JSON null崩溃、v3 ANR；assets重组为res/config
- #4  2026-08-28 02:52  fix: crash(root cause Infinity-scroll)+CCW sprite rotation+settings persistence+bottom-bar 2 tabs+PRD item/recipe dialogs+NPC 3-col grid+6-block detail+schedule married/unmarried filter+timeline
- #5  2026-08-28 14:39  v5: 色彩模式6态+Monet开关+14开关+关于页; 主页3白卡; 物品6列方形; 星价卡片; 配方/NPC弹窗改版; 切后台重载修复(cachedLoadedData); 包名com.nainiuzhen.wiki
- #6  2026-08-28 20:17  build #6: v6 bug fixes - resolve 6 compile errors + feature work from bug.txt
- #8  2026-08-29 10:26  v8 bug-v7改进: 启动页深色+进度条; 悬浮底栏对齐demo; 版本1.0.8; 关于页滚动显隐顶栏; NPC放大恢复+详情重排+过滤空区; 配方dialog角标+解锁左右分区; 筛选统一小胶囊; 顶栏模糊Bottom+搜索栏不透明; 计数间距紧凑
- #9  2026-08-29 10:34  v9 bugfix: 富文本繁荣度(数字无字号)误判为字号导致整段降级纯文本; 修复/#颜色#数字/#解析; 版本1.0.9
