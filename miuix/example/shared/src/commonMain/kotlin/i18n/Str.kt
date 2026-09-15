// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package i18n

/**
 * Every user-visible string of the example app, declared once with its English and Chinese
 * variant side by side. Add a new entry here (and use it through [Strings]) instead of
 * hard-coding literals in composables.
 *
 * Entries never contain a language-agnostic identifier (component names, color tokens, shader
 * uniforms, JSON keys, ...) - those stay in the source as-is.
 */
enum class Str(val en: String, val zh: String) {
    // ---- AboutPage.kt
    About("About", "关于"),
    MiuixForCompose("Miuix for Compose", "Miuix for Compose"),
    ViewSource("View Source", "查看源码"),
    JoinGroup("Join Group", "加入群组"),
    License("License", "开源许可"),
    ThirdPartyLicenses("Third Party Licenses", "第三方许可"),
    BackgroundEffect("Background Effect", "背景效果"),
    EffectVariant("Effect Variant", "效果变体"),
    DynamicBackground("Dynamic Background", "动态背景"),
    FullScreenBackground("Full Screen Background", "全屏背景"),

    // ---- AppContent.kt
    Home("Home", "主页"),
    Icon("Icon", "图标"),
    Color("Color", "颜色"),
    TextStyle("TextStyle", "文字样式"),
    Settings("Settings", "设置"),
    Edit("Edit", "编辑"),
    Delete("Delete", "删除"),
    More("More", "更多"),

    // ---- ColorPage.kt
    CurrentThemeColors("Current Theme Colors", "当前主题颜色"),
    LightThemeColors("Light Theme Colors", "浅色主题颜色"),
    DynamicLightColors("Dynamic Light Colors", "动态浅色"),
    DarkThemeColors("Dark Theme Colors", "深色主题颜色"),
    DynamicDarkColors("Dynamic Dark Colors", "动态深色"),

    // ---- IconPage.kt
    SearchIcons("Search icons", "搜索图标"),
    Light("Light", "浅色"),
    Name("Name", "名称"),
    TapToCompareWeights("Tap to compare weights", "点击对比字重"),
    Collapse("Collapse", "折叠"),
    Expand("Expand", "展开"),

    // ---- MainPage.kt
    SelectionA1("Selection A-1", "选项 A-1"),
    SelectionA2("Selection A-2", "选项 A-2"),
    SelectionB1("Selection B-1", "选项 B-1"),
    SelectionB2("Selection B-2", "选项 B-2"),
    SelectionB3("Selection B-3", "选项 B-3"),
    SelectionC1("Selection C-1", "选项 C-1"),
    SelectionC2("Selection C-2", "选项 C-2"),
    SelectionC3("Selection C-3", "选项 C-3"),
    SelectionC4("Selection C-4", "选项 C-4"),
    SortByCaptureDate("Sort by capture date", "按拍摄日期排序"),
    SortByDateAdded("Sort by date added", "按添加日期排序"),
    GroupByDate("Group by date", "按日期分组"),
    Compact("Compact", "紧凑"),
    AllItems("All items", "全部项目"),
    CameraAlbum("Camera album", "相机相册"),
    CollapseOnSelection("Collapse On Selection", "选择后折叠"),
    ViewMode("View mode", "视图模式"),
    Filter("Filter", "筛选"),
    MultiSelectionA1("Multi selection A-1", "多选项 A-1"),
    MultiSelectionB2("Multi selection B-2", "多选项 B-2"),
    MultiSelectionB3("Multi selection B-3", "多选项 B-3"),
    MultiSelectionA2("Multi selection A-2", "多选项 A-2"),
    MultiSelectionB1("Multi selection B-1", "多选项 B-1"),
    Options("Options", "选项"),
    Tune("Tune", "调节"),
    Sort("Sort", "排序"),
    SelectAll("Select all", "全选"),
    SelectAll2("SelectAll", "全选"),
    Search("Search", "搜索"),
    Cancel("Cancel", "取消"),

    // ---- MultiScaffoldTestPage.kt
    MultiScaffoldTest("Multi-Scaffold Test", "多 Scaffold 测试"),
    TopLeft("Top Left", "左上"),
    Dropdown("Dropdown", "下拉菜单"),
    TopRight("Top Right", "右上"),
    BottomLeft("Bottom Left", "左下"),
    BottomRight("Bottom Right", "右下"),

    // ---- NavigateTestPage.kt
    ContinuousDepth("Continuous depth", "连续深度"),
    PushAnotherNavigationPage("Push another Navigation page", "再入栈一个 Navigation 页面"),
    SinglePushAnimatedTopNN1("Single push (animatedTop N -> N+1)", "单级入栈（animatedTop N -> N+1）"),
    PushThreePagesAtOnce("Push three pages at once", "一次入栈三个页面"),
    ContinuousMultiPushNN3OneSharedSpring("Continuous multi-push (N -> N+3, one shared spring)", "连续多级入栈（N -> N+3，共用一个弹簧）"),
    PopAllNavigationPages("Pop all Navigation pages", "弹出所有 Navigation 页面"),
    ContinuousMultiPopBackToTheEntry("Continuous multi-pop back to the entry", "连续多级返回至入口页"),
    GestureBack("Gesture back", "手势返回"),
    SwipeToGoBack("Swipe to go back", "滑动返回"),
    LayoutTest("Layout test", "布局测试"),
    Summary("Summary", "摘要"),
    Start("Start", "起点"),
    Title("Title", "标题"),

    // ---- NestedNavTestPage.kt
    BackSemantics("Back semantics", "返回语义"),
    OneBackStreamNested("One back stream, nested", "单一返回流，嵌套"),
    PushInnerLevel("Push inner level", "入栈内层"),
    SystemBackPopsItBeforeThisPage("System back pops it before this page", "系统返回会先弹出它，再弹出本页"),
    PopInnerLevel("Pop inner level", "弹出内层"),
    PushAnOuterPageOnTop("Push an outer page on top", "在上层入栈外层页面"),
    BackThenPopsTheOuterPageNotThisStack("Back then pops the outer page, not this stack", "此时返回弹出的是外层页面，而非此内层栈"),

    // ---- OverscrollLoadMorePage.kt
    OverscrollLoadMore("Overscroll + Load More", "回弹 + 加载更多"),
    FlingToTheBottomFastNewItemsLoadWhileTheBoun("Fling to the bottom fast: new items load while the bounce-back plays. ", "快速滑到底部：回弹动画播放时新条目会继续加载。"),
    FlingAgainInertialScrollingMustKeepWorking("Fling again — inertial scrolling must keep working.", "再次滑动 —— 惯性滚动应仍然有效。"),
    LoadingMore("Loading more…", "正在加载更多…"),

    // ---- PullToRefreshPage.kt
    Option1("Option 1", "选项 1"),
    Option2("Option 2", "选项 2"),
    Option3("Option 3", "选项 3"),
    Option4("Option 4", "选项 4"),
    Popup("Popup", "弹窗"),
    Refresh("Refresh", "刷新"),
    PullToRefreshSettings("PullToRefresh Settings", "下拉刷新设置"),
    RefreshThreshold("Refresh Threshold", "刷新阈值"),
    AnyPullTriggersRefresh("Any pull triggers refresh.", "任意下拉都会触发刷新。"),

    // ---- SettingsPage.kt
    Language("Language", "语言"),
    LanguageSystem("System", "跟随系统"),
    Default("Default", "默认"),
    Horizontal("Horizontal", "水平"),
    Vertical("Vertical", "垂直"),
    System("System", "跟随系统"),
    Dark("Dark", "深色"),
    MonetSystem("MonetSystem", "动态取色（跟随系统）"),
    MonetLight("MonetLight", "动态取色（浅色）"),
    MonetDark("MonetDark", "动态取色（深色）"),
    Gaussian("Gaussian", "高斯"),
    Progressive("Progressive", "渐进"),
    ShowFPSMonitor("Show FPS Monitor", "显示 FPS 监视器"),
    ColorMode("Color Mode", "颜色模式"),
    KeyColor("Key Color", "主题色"),
    PaletteStyle("Palette Style", "调色板风格"),
    ColorSpec("Color Spec", "颜色规范"),
    EnableSquircleShapes("Enable Squircle Shapes", "启用方形圆角"),
    EnableBlurEffect("Enable Blur Effect", "启用模糊效果"),
    EnableScrollEndHaptic("Enable Scroll End Haptic", "滑动到底触感反馈"),
    EnablePageUserScroll("Enable Page User Scroll", "允许用户滑动页面"),
    ShowTopAppBar("Show TopAppBar", "显示顶栏"),
    TopAppBarBlurStyle("TopAppBar Blur Style", "顶栏模糊样式"),
    ShowNavigationRail("Show NavigationRail", "显示导航导轨"),
    ShowNavigationBar("Show NavigationBar", "显示导航栏"),
    ShowNavigationBadge("Show Navigation Badge", "显示导航角标"),
    NavigationBarMode("NavigationBar Mode", "导航栏模式"),
    UseFloatingNavigationBar("Use FloatingNavigationBar", "使用悬浮导航栏"),
    FloatingNavigationBarStyle("FloatingNavigationBar Style", "悬浮导航栏样式"),
    FloatingNavigationBarPosition("FloatingNavigationBar Position", "悬浮导航栏位置"),
    ShowFloatingToolbar("Show FloatingToolbar", "显示悬浮工具栏"),
    FloatingToolbarPosition("FloatingToolbar Position", "悬浮工具栏位置"),
    FloatingToolbarOrientation("FloatingToolbar Orientation", "悬浮工具栏方向"),
    ShowFloatingActionButton("Show FloatingActionButton", "显示悬浮按钮"),
    FloatingActionButtonPosition("FloatingActionButton Position", "悬浮按钮位置"),
    Navigation("Navigation", "导航"),
    TransitionStyle("Transition Style", "转场样式"),
    EnableCornerClip("Enable Corner Clip", "启用圆角裁剪"),
    ClipTheTopSceneWithRoundedCornersDuringTrans("Clip the top scene with rounded corners during transitions", "转场时对上层页面做圆角裁剪"),
    EnableDim("Enable Dim", "启用压暗"),
    DimTheSceneBehindDuringTransitions("Dim the scene behind during transitions", "转场时压暗后方页面"),
    BlockInputDuringTransition("Block Input During Transition", "转场期间阻止输入"),
    BlockTouchInputOnTheNonTargetScene("Block touch input on the non-target scene", "阻止非目标页面的触摸输入"),
    EnableSwipeBack("Enable Swipe Back", "启用滑动返回"),
    SwipeAPushedPageToPopItDirectionFollowsLayou("Swipe a pushed page to pop it; direction follows layout", "滑动已入栈页面即可返回；方向跟随布局"),
    Other("Other", "其他"),
    AboutThisExampleApp("About this example App", "关于此示例应用"),

    // ---- TextStylePage.kt
    TextStyle2("Text Style", "文字样式"),
    TitleStyles("Title Styles", "标题样式"),
    HeadlineStyles("Headline Styles", "大标题样式"),
    BodyStyles("Body Styles", "正文样式"),
    FootnoteStyles("Footnote Styles", "脚注样式"),
    AllStylesOverview("All Styles Overview", "全部样式总览"),

    // ---- component/ArrowSection.kt
    Arrow("Arrow", "Arrow"),
    Personal("Personal", "个人"),
    Volume("Volume", "音量"),
    DisabledArrow("Disabled Arrow", "Arrow（禁用）"),
    AdjustVolume("Adjust Volume", "调节音量"),
    Enter0100("Enter 0-100", "请输入 0-100"),
    Confirm("Confirm", "确定"),

    // ---- component/BadgeSection.kt
    Messages("Messages", "消息"),
    Email("Email", "邮件"),
    Favorites("Favorites", "收藏"),

    // ---- component/BlurSection.kt
    TextureBlur("Texture Blur", "纹理模糊"),
    ForegroundBlur("Foreground Blur", "前景模糊"),
    ProgressiveBlur("Progressive Blur", "渐进模糊"),
    None("None", "无"),
    Top("Top", "顶部"),
    Bottom("Bottom", "底部"),
    Left("Left", "左侧"),
    Right("Right", "右侧"),
    Direction("Direction", "方向"),
    BlendMode("Blend Mode", "混合模式"),
    BlurRadius("Blur Radius", "模糊半径"),
    Noise("Noise", "噪点"),
    Curve("Curve", "曲线"),
    Highlight("Highlight", "高光"),
    Brightness("Brightness", "亮度"),
    Contrast("Contrast", "对比度"),
    Saturation("Saturation", "饱和度"),
    LogoBlend("Logo Blend", "Logo 混合"),

    // ---- component/BottomSheetSection.kt
    ClickToShowAnOverlayBottomSheet("Click to show an OverlayBottomSheet", "点击显示 OverlayBottomSheet"),
    ClickToShowAWindowBottomSheet("Click to show a WindowBottomSheet", "点击显示 WindowBottomSheet"),
    BehaviorSettings("Behavior Settings", "行为设置"),
    AllowDismiss("Allow Dismiss", "允许关闭"),
    DragOrBackToDismiss("Drag or Back to dismiss", "拖动或返回键关闭"),
    EnableNestedScroll("Enable NestedScroll", "启用嵌套滚动"),
    ScrollContentVsDragSheet("Scroll content vs Drag sheet", "内容滚动 vs 拖动面板"),
    TextField("TextField", "TextField"),
    SwitchPref("SwitchPref", "SwitchPref"),

    // ---- component/BreadcrumbBarSection.kt
    InternalStorage("Internal storage", "内部存储"),
    DataBackup("DataBackup", "数据备份"),

    // ---- component/ButtonSection.kt
    Submit("Submit", "提交"),
    Disabled("Disabled", "禁用"),

    // ---- component/CardSection.kt
    ShowIndicationTrue("ShowIndication: true", "ShowIndication: true"),
    CardClick("Card click", "卡片点击"),
    PressFeedbackNTypeSink("PressFeedback\nType: Sink", "PressFeedback\n类型：Sink"),
    CardLongPress("Card long press", "卡片长按"),
    PressFeedbackNTypeTilt("PressFeedback\nType: Tilt", "PressFeedback\n类型：Tilt"),
    LongPressToShowDialog("Long press to show dialog", "长按显示对话框"),
    LongPressAction("Long Press Action", "长按操作"),
    TriggeredByLongPressingTheCard("Triggered by long pressing the card.", "由长按卡片触发。"),

    // ---- component/CheckboxSection.kt
    DisabledCheckbox("Disabled Checkbox", "Checkbox（禁用）"),

    // ---- component/DialogSection.kt
    ClickToShowAnOverlayDialog("Click to show an OverlayDialog", "点击显示 OverlayDialog"),
    ClickToShowAWindowDialog("Click to show a WindowDialog", "点击显示 WindowDialog"),
    PortraitShowsARegularDialogLandscapeShowsATw("Portrait shows a regular dialog; landscape shows a two-column dialog", "竖屏为普通对话框；横屏为双列对话框"),
    ForceTheLargeScreenPresentationWithLargeScre("Force the large-screen presentation with largeScreen = true", "使用 largeScreen = true 强制大屏样式"),
    ADialogComponentInsideMiuixPopupHost("A dialog component inside MiuixPopupHost.", "位于 MiuixPopupHost 内的对话框组件。"),
    AWindowLevelDialogNoMiuixPopupHostRequired("A window-level dialog, no MiuixPopupHost required.", "窗口级对话框，无需 MiuixPopupHost。"),
    CenteredDialog("Centered Dialog", "居中对话框"),
    LargeScreenTrueForcesTheCenteredPresentation("largeScreen = true forces the centered presentation on any window size.", "largeScreen = true 会在任意窗口尺寸下强制居中样式。"),
    WideDialog("Wide Dialog", "宽屏对话框"),
    RotateToLandscapeToSeeTheEffect("Rotate to landscape to see the effect.", "旋转到横屏查看效果。"),
    AllowOnce("Allow Once", "仅允许一次"),
    AlwaysAllow("Always Allow", "始终允许"),
    Deny("Deny", "拒绝"),

    // ---- component/DropdownSection.kt
    OptionA1("Option A-1", "选项 A-1"),
    OptionA2("Option A-2", "选项 A-2"),
    OptionB1("Option B-1", "选项 B-1"),
    OptionB2("Option B-2", "选项 B-2"),
    OptionB3("Option B-3", "选项 B-3"),
    OptionC1("Option C-1", "选项 C-1"),
    OptionC2("Option C-2", "选项 C-2"),
    OptionC3("Option C-3", "选项 C-3"),
    OptionC4("Option C-4", "选项 C-4"),
    Expanded("Expanded", "已展开"),
    Collapsed("Collapsed", "已折叠"),

    // ---- component/OtherPageSection.kt
    PullToRefreshTest("PullToRefresh Test", "PullToRefresh 测试"),
    NavigateToAPullToRefreshPage("Navigate to a PullToRefresh Page", "跳转到 PullToRefresh 页面"),
    NavigationTest("Navigation test", "Navigation 测试"),
    NavigateToANavigationPage("Navigate to a Navigation Page", "跳转到 Navigation 页面"),
    MultiScaffoldTest2("MultiScaffold Test", "MultiScaffold 测试"),
    NavigateToAMultiScaffoldPage("Navigate to a MultiScaffold Page", "跳转到 MultiScaffold 页面"),
    NestedNavigationTest("Nested Navigation Test", "嵌套导航测试"),
    ANavDisplayNestedInsideAnEntry("A NavDisplay nested inside an entry", "嵌套在条目内的 NavDisplay"),
    OverscrollLoadMoreTest("Overscroll + Load More Test", "回弹 + 加载更多测试"),
    FlingToTheBottomThenFlingAgain("Fling to the bottom, then fling again", "滑到底部后再滑一次"),

    // ---- component/RadioButtonSection.kt
    DisabledRadioButton("Disabled RadioButton", "RadioButton（禁用）"),
    ThisOptionIsUnavailable("This option is unavailable", "该选项不可用"),
    OptionA("Option A", "选项 A"),
    OptionB("Option B", "选项 B"),
    OptionC("Option C", "选项 C"),

    // ---- component/SliderSection.kt
    Steps("Steps", "步进"),
    StepsWithKeyPoints("Steps with Key Points", "带关键点的步进"),
    CustomKeyPoints("Custom Key Points", "自定义关键点"),
    Range("Range", "区间"),
    RangeWithKeyPoints("Range with Key Points", "带关键点的区间"),
    CustomRangePoints("Custom Range Points", "自定义区间点"),

    // ---- component/SnackbarSection.kt
    DismissOldest("Dismiss oldest", "关闭最早"),
    DismissNewest("Dismiss newest", "关闭最新"),
    Short4s("Short (4s)", "短（4 秒）"),
    ThisMessageStaysFor4Seconds("This message stays for 4 seconds.", "此消息停留 4 秒。"),
    Long10s("Long (10s)", "长（10 秒）"),
    ThisIsALongerMessageThatStaysFor10Seconds("This is a longer message that stays for 10 seconds.", "这是一条停留 10 秒的较长消息。"),
    Custom2s("Custom (2s)", "自定义（2 秒）"),
    ThisMessageUsesACustom2SecondDuration("This message uses a custom 2-second duration.", "此消息使用自定义的 2 秒时长。"),
    Action("Action", "操作"),
    ActionAlive("Action: Alive", "操作：有效"),
    ThisMessageHasAnActionButton("This message has an action button.", "此消息带有一个操作按钮。"),
    Undo("Undo", "撤销"),
    ActionUndo("Action: Undo", "操作：撤销"),
    ActionExpired("Action: Expired", "操作：已过期"),
    Dismissible("Dismissible", "可关闭"),
    TapTheCloseButtonToDismissThisMessage("Tap the close button to dismiss this message.", "点击关闭按钮关闭此消息。"),
    Indefinite("Indefinite", "常驻"),
    ThisMessageStaysUntilYouDismissItManually("This message stays until you dismiss it manually.", "此消息会一直显示，直到你手动关闭。"),
    ActionClose("Action + Close", "操作 + 关闭"),
    ThisMessageHasBothAnActionAndACloseButton("This message has both an action and a close button.", "此消息同时带有操作按钮和关闭按钮。"),

    // ---- component/SpinnerSection.kt
    // (Green / Blue / Yellow are shared with the key-color set declared under `ui/Theme.kt` below.)
    Red("Red", "红色"),
    AsDialogO("As Dialog (O)", "以对话框形式 (O)"),
    OK("OK", "确定"),
    AsDialogW("As Dialog (W)", "以对话框形式 (W)"),
    Option5("Option 5", "选项 5"),
    Option6("Option 6", "选项 6"),

    // ---- component/SuperSearchBar.kt
    Back("back", "返回"),
    Clean("Clean", "清除"),

    // ---- component/SwitchSection.kt
    False("false", "false"),
    ClickToExpandASwitch("Click to expand a Switch", "点击展开 Switch"),
    DisabledSwitch("Disabled Switch", "Switch（禁用）"),

    // ---- component/TabRowSection.kt
    Tab1("Tab 1", "标签 1"),
    Tab2("Tab 2", "标签 2"),
    Tab3("Tab 3", "标签 3"),
    Tab4("Tab 4", "标签 4"),
    Tab5("Tab 5", "标签 5"),
    Tab6("Tab 6", "标签 6"),

    // ---- component/TextFieldSection.kt
    WithTitle("With title", "带标题"),
    StateBased("State-based", "基于状态"),
    PlaceholderSingleLine("Placeholder & SingleLine", "占位符 & 单行"),

    // ---- component/TooltipSection.kt
    RichTooltip("Rich tooltip", "富文本提示"),
    RichTooltipsShowATitleSupportingTextAndAnOpt("Rich tooltips show a title, supporting text, and an optional action. ", "富文本提示可显示标题、说明文字和可选操作。"),
    MoveOntoTheTooltipToUseTheActionOrTapOutside("Move onto the tooltip to use the action, or tap outside to dismiss.", "将光标移到提示上以使用操作，或点击外部关闭。"),
    GotIt("Got it", "知道了"),

    // ---- component/highlight/HighlightConfig.kt
    LargeContainer("Large Container", "大容器"),
    MediumContainer("Medium Container", "中容器"),
    SmallContainer("Small Container", "小容器"),

    // ---- ui/Theme.kt
    Purple("Purple", "紫色"),
    Orange("Orange", "橙色"),
    Pink("Pink", "粉色"),
    Teal("Teal", "青色"),
    Blue("Blue", "蓝色"),
    Green("Green", "绿色"),
    Yellow("Yellow", "黄色"),

    // ---- templates; use `s.format(Str.X, arg0, arg1, ...)`
    VersionLine("v{0} ({1})", "v{0}（{1}）"),
    SuggestionIndex("Suggestion {0}", "建议 {0}"),
    NavigateTestTitle("Navigate Test {0}", "导航测试 {0}"),
    NestedLevel("Level {0}", "层级 {0}"),
    ItemIndex("Item {0}", "条目 {0}"),
    ItemsPagesLoaded("Items: {0} · Pages loaded: {1}", "条目：{0} · 已加载页数：{1}"),
    PullProgress("Pull Progress: {0}%", "下拉进度：{0}%"),
    Threshold("Threshold: {0}%", "阈值：{0}%"),
    PullToRefreshHint("Pull {0}% of the drag range to refresh.", "下拉至拖动范围的 {0}% 触发刷新。"),
    ClickCount("Click: {0}", "点击：{0}"),
    CheckedState("State: {0}", "状态：{0}"),
    SelectedState("Selected: {0}", "已选中：{0}"),
    CurrentPath("Current: {0}", "当前：{0}"),
    FullPath("Full path: {0}", "完整路径：{0}"),
    TabContent("Content of {0}", "{0} 的内容"),
    OptionIndex("Option {0}", "选项 {0}"),
    SpinnerSuffixExpanded(" (Expanded)", "（已展开）"),
    SpinnerSuffixCollapsed(" (Collapsed)", "（已折叠）"),
    IconWeightLabel("{0} ({1})", "{0}（{1}）"),
    ProgressiveBlurValue("Progressive Blur\n{0} | R={1} | {2}", "渐进模糊\n{0} | R={1} | {2}"),
    TextureBlurMeasure("Texture Blur | R={0}", "纹理模糊 | R={0}"),
    ForegroundBlurDemo("Foreground Blur\nMiuix Demo", "前景模糊\nMiuix Demo"),
    VerticalNormal("Normal\n{0}", "正常\n{0}"),
    VerticalSteps("Steps\n{0}", "步进\n{0}"),
    VerticalPoints("Points\n{0}", "关键点\n{0}"),
    VerticalCustom("Custom\n{0}", "自定义\n{0}"),
    VerticalDisabled("Disabled\n{0}", "禁用\n{0}"),
    SwipeBackHint("Turn on \"Enable Swipe Back\" in Settings, then swipe a pushed page to pop it. Stack several pages with continuous push above and drag back through them; the gesture drives the same animatedTop spring.", "先在设置中打开「启用滑动返回」，然后滑动已入栈的页面即可返回。用上面的连续入栈堆叠多个页面，再拖拽返回；手势驱动的是同一个 animatedTop 弹簧。"),
    NestedBackHint("System back pops the inner stack below first. At inner level 1 it falls through and pops this page. While another page covers this one, back never touches the inner stack.", "系统返回会先弹出下方的内层栈。到内层第 1 层时会继续冒泡并弹出本页。当另一个页面覆盖本页时，返回不会触碰到内层栈。"),

    // ---- second leftover pass: section titles, demo titles, blur layers, misc

    // Section titles (SmallTitle)
    Badge("Badge", "徽标"),
    BasicComponent("Basic Component", "基础组件"),
    BottomSheet("BottomSheet", "底部弹窗"),
    BreadcrumbBar("BreadcrumbBar", "面包屑栏"),
    Button("Button", "按钮"),
    Card("Card", "卡片"),
    Checkbox("Checkbox", "复选框"),
    Dialog("Dialog", "对话框"),
    RadioButton("RadioButton", "单选按钮"),
    Slider("Slider", "滑块"),
    Snackbar("Snackbar", "Snackbar"),
    Spinner("Spinner", "下拉选择器"),
    Switch("Switch", "开关"),
    TabRow("TabRow", "标签栏"),
    Tooltip("Tooltip", "工具提示"),

    // Demo titles with overlay/window variants
    DialogO("Dialog (O)", "对话框 (O)"),
    DialogW("Dialog (W)", "对话框 (W)"),
    WideDialogO("Wide Dialog (O)", "宽对话框 (O)"),
    WideDialogW("Wide Dialog (W)", "宽对话框 (W)"),
    CenteredDialogO("Centered Dialog (O)", "居中对话框 (O)"),
    BottomSheetO("BottomSheet (O)", "底部弹窗 (O)"),
    BottomSheetW("BottomSheet (W)", "底部弹窗 (W)"),
    DropdownPrefO("DropdownPref (O)", "下拉首选项 (O)"),
    DropdownPrefW("DropdownPref (W)", "下拉首选项 (W)"),
    GroupedDropdownPrefO("Grouped DropdownPref (O)", "分组下拉首选项 (O)"),
    GroupedDropdownPrefW("Grouped DropdownPref (W)", "分组下拉首选项 (W)"),
    DisabledDropdownPrefO("Disabled DropdownPref (O)", "禁用的下拉首选项 (O)"),
    DisabledDropdownPrefW("Disabled DropdownPref (W)", "禁用的下拉首选项 (W)"),
    SpinnerPrefO("SpinnerPref (O)", "下拉选择器首选项 (O)"),
    SpinnerPrefW("SpinnerPref (W)", "下拉选择器首选项 (W)"),
    GroupedSpinnerPrefO("Grouped SpinnerPref (O)", "分组下拉选择器首选项 (O)"),
    GroupedSpinnerPrefW("Grouped SpinnerPref (W)", "分组下拉选择器首选项 (W)"),
    DisabledSpinnerPrefO("Disabled SpinnerPref (O)", "禁用的下拉选择器首选项 (O)"),
    DisabledSpinnerPrefW("Disabled SpinnerPref (W)", "禁用的下拉选择器首选项 (W)"),

    // Slider demos
    Normal("Normal", "常规"),
    RangeSlider("RangeSlider", "范围滑块"),
    VerticalSlider("VerticalSlider", "纵向滑块"),

    // Blur demo layers
    BlurInfoThin("Info Thin", "信息-细"),
    BlurInfoRegular("Info Regular", "信息-常规"),
    BlurColoredThin("Colored Thin", "着色-细"),
    BlurColoredRegular("Colored Regular", "着色-常规"),
    BlurColoredThick("Colored Thick", "着色-厚"),
    BlurPuredRegular("Pured Regular", "纯色-常规"),
    BlurPuredThick("Pured Thick", "纯色-厚"),
    BlurOverlayThin("Overlay Thin", "覆盖-细"),
    BlurOverlayThick("Overlay Thick", "覆盖-厚"),
    BlurInfoColored("Info Colored", "信息-着色"),

    // Long / trailing demo texts
    End("End", "结尾"),
    End1("End1", "结尾 1"),
    End2("End2", "结尾 2"),
    LongTitle("Long Long Long Long Long Title", "很长很长的标题"),
    LongSummary("Long Long Long Long Long Summary", "很长很长的摘要"),
    LongEnd("Long Long Long Long Long End", "很长很长的结尾"),
    LongOption2("Long Option 2", "长选项 2"),
    LongOption3("Long Long Option 3", "长长选项 3"),
    LongOption4("Long Long Long Option 4", "长长长选项 4"),
    LongOption5("Long Long Long Long Option 5", "长长长长选项 5"),
    LongOption6("Long Long Long Long Long Option 6", "长长长长长选项 6"),
    LongOption7("Long Long Long Long Long Long Option 7", "长长长长长长选项 7"),
    LongOption8("Long Long Long Long Long Long Long Option 8", "长长长长长长长选项 8"),
    LongOption9("Long Long Long Long Long Long Long Long Option 9", "长长长长长长长长选项 9"),
    LongOption10("Long Long Long Long Long Long Long Long Long Option 10", "长长长长长长长长长选项 10"),
    LongOption11("Long Long Long Long Long Long Long Long Long Long Option 11", "长长长长长长长长长长选项 11"),
    LongOption12("Long Long Long Long Long Long Long Long Long Long Long Option 12", "长长长长长长长长长长长选项 12"),

    // Nested navigation
    NestedNavigation("Nested Navigation", "嵌套导航"),
    InnerStack("Inner stack", "内层返回栈"),

    // Text style specs
    TextStyleBoldSpec("14sp / Bold", "14sp / 粗体"),
    TextStyleLineHeightSpec("17sp / lineHeight 1.2em", "17sp / 行高 1.2em"),

    // FPS monitor
    FpsAvgPrefix("AVG ", "平均 "),
    FpsLowPrefix("LOW ", "最低 "),

    // Pull-to-refresh list demos
    OverlayDropdownPrefN("OverlayDropdownPref {0}", "覆盖层下拉首选项 {0}"),
    WindowDropdownPrefN("WindowDropdownPref {0}", "窗口下拉首选项 {0}"),
}
