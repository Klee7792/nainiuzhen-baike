import UIKit
import SwiftUI
import shared

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            // 全屏绘制：内容延伸到状态栏与小白条（home indicator）之下，
            // 安全区由 Compose 侧的 WindowInsets（statusBars/navigationBars）消费——
            // miuix TopAppBar 顶部、NavigationBar / 液态玻璃底栏底部均已内置该适配。
            // 包含 keyboard region：键盘 insets 同样由 Compose 自己处理。
            .ignoresSafeArea()
    }
}
