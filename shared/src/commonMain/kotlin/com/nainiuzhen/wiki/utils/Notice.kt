package com.nainiuzhen.wiki.utils

/**
 * 本应用公开仓地址。
 *
 * ⚠️ **全应用唯一来源**：关于页的「查看源码」/「问题反馈」两行、以及免责声明里的联系方式
 * 都必须引用这里，**不要再各写一份字面量**。
 * （历史教训：关于页「查看源码」曾残留 miuix demo 的上游地址
 * `github.com/compose-miuix-ui/miuix`，纯属字面量散落导致的漏改。）
 */
const val PROJECT_REPO_URL: String = "https://github.com/Klee7792/nainiuzhen-baike"

/**
 * 仓库 Issues 页 —— 全应用**唯一对外联系通道**。
 *
 * 用途：① 免责声明的「权利人可以联系到我们」落地；② 关于页「问题反馈」入口。
 *
 * ⚠️ 免责声明里写「请联系开发者」却不给可达渠道，等于这条通道是空的 —— 权利人只能转向
 * 平台投诉，声明的实际价值大打折扣。所以本常量与 [NOTICE_TEXT] 是**强绑定**的，
 * 改动其中一方必须同步另一方。
 */
const val PROJECT_ISSUES_URL: String = PROJECT_REPO_URL + "/issues"

/**
 * 「使用须知 / 免责声明」当前版本号。
 *
 * 语义：用户「已同意的版本」存在 `AppState.agreedNoticeVersion`；当
 * `agreedNoticeVersion < NOTICE_VERSION` 时，应用进入主界面后强制弹出
 * [com.nainiuzhen.wiki.ui.components.NoticeDialog]，必须点「同意并继续」才放行。
 * 没有该记录（首次安装、或从没有本功能的旧版本升级上来）一律视为 0 ⇒ 会弹一次，
 * 即「没有已同意的记录就当首次打开处理」。
 *
 * ⚠️ **改动下方 [NOTICE_TEXT] 正文后必须把本常量 +1**，否则已同意过的老用户
 * 不会再确认一次（等于白改）。
 */
const val NOTICE_VERSION: Int = 1

/** 首次启动强制弹窗的标题。 */
const val NOTICE_TITLE: String = "使用须知"

/** 关于页常驻卡片的小标题（正文同 [NOTICE_TEXT]，**不要另写一份**）。 */
const val NOTICE_CARD_TITLE: String = "免责声明"

/**
 * 免责声明正文段落 —— **全应用唯一文案来源**，共两处使用方：
 *
 * 1. 首次启动强制弹窗：`ui/components/NoticeDialog.kt`（段落逐段渲染 + 「不同意并退出 / 同意并继续」）；
 * 2. 关于页常驻卡片：`ui/settings/AboutScreen.kt`（段落之间空一行，只读展示）；
 * 3. 另需与仓库根 `README.md` 的「免责声明」小节逐条一致。
 *
 * 写作约束（改动时请一并遵守）：
 * - **不要**写「本应用与原作者无关」这类措辞 —— 反而容易被认定为「明知故犯」；
 * - 「24 小时内删除」降低的是「主观恶意」的认定，**不是免责金牌**，不要写成免责承诺；
 * - 最后一段的「继续使用即表示同意」是弹窗留痕的落点，**不要删**；
 * - 第 5 段必须给出**可达**的联系渠道（见 [PROJECT_ISSUES_URL]），否则整段失去意义。
 *
 * 「本应用完全离线运行」一句与事实绑定：`AndroidManifest.xml` 当前**零** `uses-permission`，
 * 全仓无网络代码。若将来加入任何联网能力，**必须同步改掉这一段**并 +1 [NOTICE_VERSION]。
 */
val NOTICE_TEXT: List<String> = listOf(
    "本应用为非官方、非营利的爱好者作品，仅供个人学习与技术交流使用。",
    "应用内展示的游戏图像、图标、文本及数据均来源于网络，版权归原游戏开发/发行方所有，本应用不主张任何权利。",
    "请于下载后 24 小时内自行删除本应用及其中素材；禁止用于任何商业用途，禁止二次打包分发。",
    "本应用完全离线运行，不联网、不收集、不上传任何个人信息；您的设置仅保存在本机。",
    "如权利人认为本应用侵犯了您的合法权益，请在项目仓库提交 Issue（$PROJECT_ISSUES_URL）" +
        "或通过仓库主页联系开发者，我们将立即停止分发并删除相关内容。",
    "继续使用本应用，即表示您已阅读并同意以上全部内容。",
)
