import SwiftUI

@main
struct TasksApp: App {
    @State private var isLoading = true
    private let ditto = DittoManager.shared.ditto

    var body: some Scene {
        WindowGroup {
            TasksListScreen()
        }
    }
}
