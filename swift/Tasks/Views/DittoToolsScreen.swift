import SwiftUI
import DittoAllToolsMenu

struct DittoToolsScreen: View {
    private let ditto = DittoManager.shared.ditto
    var body: some View {
        AllToolsMenu(ditto: ditto)
    }
}
