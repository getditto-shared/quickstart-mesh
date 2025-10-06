import SwiftUI
import DittoSwift

/// Main view of the app, which displays a list of tasks
struct TasksListScreen: View {
    private static let isSyncEnabledKey = "syncEnabled"
    
    @StateObject private var viewModel = TasksListScreenViewModel()

    @State private var syncEnabled: Bool = Self.loadSyncEnabledState()
    @State private var bluetoothEnabled: Bool = true
    @State private var lanEnabled: Bool = true
    @State private var awdlEnabled: Bool = true

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Transport controls at the top
                HStack(spacing: 12) {
                    VStack {
                        Button {
                            bluetoothEnabled.toggle()
                            viewModel.toggleBluetoothLE()
                        } label: {
                            ZStack {
                                Image("bluetooth")
                                    .renderingMode(.template)
                                    .resizable()
                                    .scaledToFit()
                                    .frame(width: 20, height: 20)

                                if !bluetoothEnabled {
                                    Image(systemName: "line.diagonal")
                                        .font(.system(size: 24))
                                        .rotationEffect(.degrees(90))
                                }
                            }
                        }
                        .frame(width: 44, height: 44)
                        .background(bluetoothEnabled ? Color.accentColor : Color.secondary.opacity(0.3))
                        .foregroundColor(.white)
                        .clipShape(Circle())

                        Text("Bluetooth")
                            .font(.caption)
                    }
                    
                    VStack {
                        Button {
                            lanEnabled.toggle()
                            viewModel.toggleLAN()
                        } label: {
                            Image(systemName: lanEnabled ? "wifi" : "wifi.slash")
                                .font(.system(size: 18))
                        }
                        .frame(width: 44, height: 44)
                        .background(lanEnabled ? Color.accentColor : Color.secondary.opacity(0.3))
                        .foregroundColor(.white)
                        .clipShape(Circle())
                        
                        Text("WiFi")
                            .font(.caption)
                    }
                    
                    VStack {
                        Button {
                            awdlEnabled.toggle()
                            viewModel.toggleAWDL()
                        } label: {
                            Image(systemName: awdlEnabled ? "iphone.gen3.radiowaves.left.and.right" : "iphone.gen3.slash")
                                .symbolRenderingMode(.monochrome)
                                .font(.system(size: 18))
                        }
                        .frame(width: 44, height: 44)
                        .background(awdlEnabled ? Color.accentColor : Color.secondary.opacity(0.3))
                        .foregroundColor(.white)
                        .clipShape(Circle())
                        
                        Text("AWDL")
                            .font(.caption)
                    }
                    
                    Spacer()
                    
                    VStack {
                        Button {
                            syncEnabled.toggle()
                        } label: {
                            ZStack {
                                Image(systemName: "arrow.triangle.2.circlepath")
                                    .font(.system(size: 18))
                                
                                if !syncEnabled {
                                    Image(systemName: "line.diagonal")
                                        .font(.system(size: 24))
                                        .rotationEffect(.degrees(90))
                                }
                            }
                        }
                        .frame(width: 44, height: 44)
                        .background(syncEnabled ? Color.accentColor : Color.secondary.opacity(0.3))
                        .foregroundColor(.white)
                        .clipShape(Circle())
                        
                        Text("Sync")
                            .font(.caption)
                    }
                    
                    VStack {
                        NavigationLink {
                            DittoToolsScreen()
                        } label: {
                            Image(systemName: "wrench.and.screwdriver")
                                .font(.system(size: 18))
                        }
                        .frame(width: 44, height: 44)
                        .background(Color.accentColor)
                        .foregroundColor(.white)
                        .clipShape(Circle())
                        
                        Text("Ditto Tools")
                            .font(.caption)
                    }
                }
                .padding(.horizontal)
                .padding(.vertical, 8)
                .background(getBackgroundColor(for: viewModel.count))

                // Count display
                HStack {
                    Text("\(viewModel.count)")
                        .font(.system(size: 64, weight: .bold))
                        .foregroundStyle(.primary)
                        .padding(.horizontal)
                        .padding(.top, 8)
                    Spacer()
                }
                .background(getBackgroundColor(for: viewModel.count))

                List {
                    Section {
                        ForEach(viewModel.tasks) { task in
                            TaskRow(
                                task: task,
                                onToggle: { task in
                                    viewModel.toggleComplete(task: task)
                                },
                                onClickEdit: { task in
                                    viewModel.onEdit(task: task)
                                }
                            )
                        }
                        .onDelete(perform: deleteTaskItems)
                    }
                }
                .background(getBackgroundColor(for: viewModel.count))
                .scrollContentBackground(.hidden)
                .listStyle(.plain)
                .animation(.default, value: viewModel.tasks)
            }
            .navigationTitle("")
            .navigationBarTitleDisplayMode(.inline)
            .safeAreaInset(edge: .bottom) {
                HStack(spacing: 12) {
                    Button {
                        viewModel.onBulkAdd()
                    } label: {
                        Label("Bulk Add", systemImage: "list.bullet.rectangle")
                            .labelStyle(.titleAndIcon)
                            .frame(maxWidth: .infinity)   // make both buttons wide & even
                    }
                    .buttonStyle(.borderedProminent)
                    .controlSize(.large)

                    Button {
                        viewModel.onNewTask()
                    } label: {
                        Label("New Task", systemImage: "plus")
                            .labelStyle(.titleAndIcon)
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .controlSize(.large)
                }
                .padding(.horizontal)
                .padding(.top, 12)
                .padding(.bottom, 24)  // moves the bar up visually and gives touch target
                .background(
                    // match your dynamic background color
                    getBackgroundColor(for: viewModel.count)
                        .ignoresSafeArea(edges: .bottom)
                )
                .overlay(
                    // optional top divider line like a toolbar
                    Divider(), alignment: .top
                )
            }
            // Sheets
            .sheet(isPresented: $viewModel.isPresentingEditScreen) {
                CreateUpdateTaskView(task: viewModel.taskToEdit)
                    .environmentObject(viewModel)
            }
            .sheet(isPresented: $viewModel.isPresentingBulkAddScreen) {
                BulkCreateTaskView()
                    .environmentObject(viewModel)
            }
            .sheet(isPresented: $viewModel.isPresentingConfigurationView) {
                ConfigurationView()
                    .environmentObject(viewModel)
            }
        }
        .background(getBackgroundColor(for: viewModel.count))
        // Prefer `.task` for startup work; keeps side effects out of the body
        .task {
            // Prevent Xcode previews from syncing
            let isPreview = ProcessInfo.processInfo.environment["XCODE_RUNNING_FOR_PREVIEWS"] == "1"
            if !isPreview {
                do {
                    try viewModel.setSyncEnabled(syncEnabled)
                } catch {
                    syncEnabled = false
                }
            }
        }
        .onChange(of: syncEnabled) { _, newValue in
            do {
                try viewModel.setSyncEnabled(newValue)
                Self.saveSyncEnabledState(newValue)
            } catch {
                syncEnabled = !newValue // Revert on error
            }
        }
    }

    // MARK: - Actions

    private func deleteTaskItems(at offsets: IndexSet) {
        let deletedTasks = offsets.map { viewModel.tasks[$0] }
        for task in deletedTasks {
            viewModel.deleteTask(task)
        }
    }

    // MARK: - Persistence
    private static func loadSyncEnabledState() -> Bool {
        if UserDefaults.standard.object(forKey: isSyncEnabledKey) == nil {
            return true
        } else {
            return UserDefaults.standard.bool(forKey: isSyncEnabledKey)
        }
    }

    private static func saveSyncEnabledState(_ state: Bool) {
        UserDefaults.standard.set(state, forKey: isSyncEnabledKey)
        UserDefaults.standard.synchronize()
    }

    // MARK: - Styling

    private func getBackgroundColor(for taskCount: Int) -> Color {
        let hexColors = Env.COLORS_HEX
            .components(separatedBy: ",")
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }

        let colorIndex = taskCount % max(hexColors.count, 1)
        return Color(hex: hexColors[colorIndex])
    }
}

struct TasksListScreen_Previews: PreviewProvider {
    static var previews: some View {
        TasksListScreen()
            .environment(\.colorScheme, .light)
    }
}
