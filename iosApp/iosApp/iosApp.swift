import SwiftUI
import UIKit

/// 动态方向控制：设置页「手机横屏」开关 → shared 层写入 NSUserDefaults
/// （键 `allowPhoneLandscape`，见 PhoneOrientation.ios.kt）→ 本代理按该键返回允许方向。
/// 键不存在（全新安装）时与 AppState.allowPhoneLandscape 的新默认值一致 = 允许旋转。
final class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        supportedInterfaceOrientationsFor window: UIWindow?
    ) -> UIInterfaceOrientationMask {
        let defaults = UserDefaults.standard
        if defaults.object(forKey: "allowPhoneLandscape") != nil,
           !defaults.bool(forKey: "allowPhoneLandscape") {
            return .portrait
        }
        return .allButUpsideDown
    }
}

@main
struct iosApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

	var body: some Scene {
		WindowGroup {
			ContentView()
		}
	}
}
