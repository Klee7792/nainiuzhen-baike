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
            .ignoresSafeArea(.keyboard) // Compose 自己处理键盘
        // 注意：不 edgesIgnoringSafeArea(.all) —— 与 Android 行为对齐，内容从状态栏下开始，
        // 状态栏区域由系统底色呈现（v38 首发，安全区内绘制的打磨留待后续版本）。
    }
}
