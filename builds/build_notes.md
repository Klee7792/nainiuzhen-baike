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
- #10  2026-08-29 12:33  bug-v9: wire iOS liquid-glass bottom bar + red top-right badge; item/recipe count left-aligned; NPC dialog favor-max icon/underline/padding; schedule filter blur synced with topbar; SpriteImage async decode (startup speedup); About screen nested layerBackdrop crash fix; docs updated to v1.0.10
- #11  2026-08-29 13:10  bug-v10: 修复关于页点击闪退——根因 AboutScreen 外层大Box的 layerBackdrop(backdrop) 把消费方 Text.textureBlur(backdrop) 包入录制子树，导致 RenderThread 上 RenderEffect 无限递归 SIGSEGV 栈溢出；改为把 layerBackdrop 经 bgModifier 只录制 BgEffectBackground 背景层，前景 Text 作为兄弟节点消费同一 backdrop
- #12  2026-08-29 13:53  v12: 关于页背景色彩流动回退(低端机Compose动画渐变兜底); 设置页加回底栏角标开关; 主页三板块等高+图片xy双居中; NPC详情弹窗按钮左右对齐+分割线80%+区域外间距最小; 物品/配方计数绑定为标题第二行居中跟随; 版本随build自动+1且只出release
- #13  2026-08-29 14:29  v13: NPC详情弹窗按钮改用weight(1f)严格等宽(日程/关闭一致); 版本命名改遇10进1(build13->1.1.3)
- #14  2026-08-29 14:51  v14: 复刻 miuix demo 关于子页面布局(背景/图标/标题/版本/双卡片四条目); 条目点击 Toast 还没做; 增加 APP_VERSION_CODE 并随 build 同步
- #15  2026-08-29 15:24  v15: 关于页四个条目卡片加textureBlur毛玻璃; 滚动上拉变纯色列表页/下拉恢复OS3(搬自demo); 新增第三方开源协议子页License; 关于页返回箭头移入顶栏可点击返回; 物品/配方计数改用miuix原生subtitle跟随标题(展开左对齐/收起居中/标题不偏移)
- #16  2026-08-29 16:14  v16: 修复关于页进入时闪退（去嵌套 layerBackdrop）; 物品/配方计数 subtitle 字号减小为12sp并中心对齐; 底栏角标默认值改为 false
- #17  2026-08-29 20:37  状态栏一致性: 渐进模糊改ProgressiveBlur.Top延伸状态栏 + 关于页OS3背景延伸状态栏 + 修复#16关于页layerBackdrop递归闪退; 悬浮底栏miuix风格对齐demo(textureBlur+玻璃描边)与iOS风格恢复lens折射; 主页3板块图标加surfaceContainer圆角底托; NPC详情好感改红心矢量+顶栏边距收紧; 配方详情数量角标去胶囊缩字号; 物品/配方计数对照v15居中
