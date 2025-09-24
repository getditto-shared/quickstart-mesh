import Combine
import DittoSwift
import SwiftUI

/// View model for TasksListScreen
@MainActor
class TasksListScreenViewModel: ObservableObject {
    @Published var tasks = [TaskModel]()
    @Published var isPresentingEditScreen: Bool = false
    @Published var isPresentingBulkAddScreen: Bool = false
    private(set) var taskToEdit: TaskModel?

    private let ditto = DittoManager.shared.ditto
    private var subscription: DittoSyncSubscription?
    private var storeObserver: DittoStoreObserver?

    private let subscriptionQuery = "SELECT * from tasks"

    private let observerQuery = "SELECT * FROM tasks WHERE NOT deleted ORDER BY title ASC"

    init() {
        populateTasksCollection()

        // Register observer, which runs against the local database on this peer
        // https://docs.ditto.live/sdk/latest/crud/observing-data-changes#setting-up-store-observers
        storeObserver = try? ditto.store.registerObserver(query: observerQuery) {
            [weak self] result in
            guard let self = self else { return }
            self.tasks = result.items.compactMap {
                TaskModel($0.jsonData())
            }
        }
    }

    deinit {
        subscription?.cancel()
        subscription = nil

        storeObserver?.cancel()
        storeObserver = nil

        if ditto.isSyncActive {
            DittoManager.shared.ditto.stopSync()
        }
    }

    func setSyncEnabled(_ newValue: Bool) throws {
        if !ditto.isSyncActive && newValue {
            try startSync()
        } else if ditto.isSyncActive && !newValue {
            stopSync()
        }
    }

    private func startSync() throws {
        do {
            try ditto.startSync()

            // Register a subscription, which determines what data syncs to this peer
            // https://docs.ditto.live/sdk/latest/sync/syncing-data#creating-subscriptions
            subscription = try ditto.sync.registerSubscription(query: subscriptionQuery)
        } catch {
            print(
                "TaskListScreenVM.\(#function) - ERROR starting sync operations: \(error.localizedDescription)"
            )
            throw error
        }
    }

    private func stopSync() {
        subscription?.cancel()
        subscription = nil

        ditto.stopSync()
    }

    func toggleComplete(task: TaskModel) {
        Task {
            let done = !task.done
            let query = """
                UPDATE tasks
                SET done = :done
                WHERE _id == :_id
                """

            do {
                try await ditto.store.execute(
                    query: query,
                    arguments: ["done": done, "_id": task._id]
                )
            } catch {
                print(
                    "TaskListScreenVM.\(#function) - ERROR toggling task: \(error.localizedDescription)"
                )
            }
        }
    }

    nonisolated func saveEditedTask(_ task: TaskModel) {
        Task {
            let query = """
                UPDATE tasks SET
                    title = :title,
                    done = :done,
                    deleted = :deleted
                WHERE _id == :_id
                """

            do {
                try await ditto.store.execute(
                    query: query,
                    arguments: [
                        "title": task.title,
                        "done": task.done,
                        "deleted": task.deleted,
                        "_id": task._id
                    ]
                )
            } catch {
                print(
                    "TaskListScreenVM.\(#function) - ERROR updating task: \(error.localizedDescription)"
                )
            }
        }
    }

    nonisolated func saveNewTask(_ task: TaskModel) {
        Task {
            let newTask = task.value
            let query = "INSERT INTO tasks DOCUMENTS (:newTask)"

            do {
                try await ditto.store.execute(
                    query: query, arguments: ["newTask": newTask])
            } catch {
                print(
                    "TaskListScreenVM.\(#function) - ERROR creating new task: \(error.localizedDescription)"
                )
            }
        }
    }

    nonisolated func deleteTask(_ task: TaskModel) {
        Task {
            let query = "UPDATE tasks SET deleted = true WHERE _id = :_id"
            do {
                try await ditto.store.execute(
                    query: query, arguments: ["_id": task._id])
            } catch {
                print(
                    "TaskListScreenVM.\(#function) - ERROR deleting task: \(error.localizedDescription)"
                )
            }
        }
    }

    private nonisolated func populateTasksCollection() {
        Task {
            let initialTasks: [TaskModel] = [
                TaskModel(
                    _id: "50191411-4C46-4940-8B72-5F8017A04FA7",
                    title: "Buy groceries"),
                TaskModel(
                    _id: "6DA283DA-8CFE-4526-A6FA-D385089364E5",
                    title: "Clean the kitchen"),
                TaskModel(
                    _id: "5303DDF8-0E72-4FEB-9E82-4B007E5797F0",
                    title: "Schedule dentist appointment"),
                TaskModel(
                    _id: "38411F1B-6B49-4346-90C3-0B16CE97E174",
                    title: "Pay bills")
            ]

            for task in initialTasks {
                do {
                    try await ditto.store.execute(
                        query: "INSERT INTO tasks INITIAL DOCUMENTS (:task)",
                        arguments: [
                            "task":
                                [
                                    "_id": task._id,
                                    "title": task.title,
                                    "done": task.done,
                                    "deleted": task.deleted
                                ]
                        ]
                    )
                } catch {
                    print(
                        "TaskListScreenVM.\(#function) - ERROR creating initial task: \(error.localizedDescription)"
                    )
                }
            }
        }
    }

    func onEdit(task: TaskModel) {
        taskToEdit = task
        isPresentingEditScreen = true
    }

    func onNewTask() {
        taskToEdit = nil
        isPresentingEditScreen = true
    }
    
    func onBulkAdd() {
        isPresentingBulkAddScreen = true
    }
    
    @MainActor
    func updateSyncScopeForAll(_ scope: SyncScope) async throws {
        let q = "ALTER SYSTEM SET USER_COLLECTION_SYNC_SCOPES = :scopes"
        let scopes: [String: String] = ["tasks": scope.rawValue]
        try await ditto.store.execute(query: q, arguments: ["scopes": scopes])
    }
}

/// Main view of the app, which displays a list of tasks
struct TasksListScreen: View {
    private static let isSyncEnabledKey = "syncEnabled"
    
    @StateObject private var viewModel = TasksListScreenViewModel()
    
    @State private var syncEnabled: Bool = Self.loadSyncEnabledState()
    @State private var syncScope: SyncScope = .allPeers
    
    var body: some View {
        NavigationStack {
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
            .background(getBackgroundColor(for: viewModel.tasks.count))
            .scrollContentBackground(.hidden)
            .listStyle(.plain)
            .animation(.default, value: viewModel.tasks)
            .navigationTitle("")
            .navigationBarTitleDisplayMode(.inline)
            /*
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    VStack {
                        Text("Sync")
                            .font(.caption)
                        Toggle("Sync", isOn: $syncEnabled)
                            .labelsHidden()
                            .toggleStyle(.switch)
                            .onChange(of: syncEnabled) { newSyncEnabled in
                                Self.saveSyncEnabledState(newSyncEnabled)
                                do {
                                    try viewModel.setSyncEnabled(newSyncEnabled)
                                } catch {
                                    // Roll back UI state on error
                                    syncEnabled = false
                                }
                            }
                    }
                }
            }
             */
            .safeAreaInset(edge: .top) {
                VStack(alignment: .leading, spacing: 8) {
                    Picker("Sync Scope", selection: $syncScope) {
                        ForEach(SyncScope.allCases) { scope in
                            Text(scope.title).tag(scope)
                        }
                    }
                    .pickerStyle(.segmented)
                    .padding(.horizontal)
                    .onChange(of: syncScope) { scope in
                        Task {
                            do {
                                try await viewModel.updateSyncScopeForAll(scope)
                            } catch {
                                print("Failed to update sync scope: \(error)")
                            }
                        }
                    }
                    HStack {
                        Text("\(viewModel.tasks.count)")
                            .font(.system(size: 64, weight: .bold))
                            .foregroundStyle(.primary)
                            .padding(.horizontal)
                            .padding(.top, 8)
                        Spacer()
                    }
                }
                .padding(.top, 12)
                .background(.clear)
            }
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
                    getBackgroundColor(for: viewModel.tasks.count)
                        .ignoresSafeArea(edges: .bottom)
                )
                .overlay(
                    // optional top divider line like a toolbar
                    Divider(), alignment: .top
                )
            }
            // Sheets
            .sheet(isPresented: $viewModel.isPresentingEditScreen) {
                EditScreen(task: viewModel.taskToEdit)
                    .environmentObject(viewModel)
            }
            .sheet(isPresented: $viewModel.isPresentingBulkAddScreen) {
                BulkAddScreen()
                    .environmentObject(viewModel)
            }
        }
        .background(getBackgroundColor(for: viewModel.tasks.count))
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

        TasksListScreen()
            .environment(\.colorScheme, .dark)
    }
}

enum SyncScope: String, CaseIterable, Identifiable {
    case allPeers = "AllPeers"
    case bigPeersOnly = "BigPeerOnly"
    case smallPeersOnly = "SmallPeersOnly"
    case localOnly = "LocalPeerOnly"

    var id: String { rawValue }

    var title: String {
        switch self {
        case .allPeers: return "All Peers"
        case .bigPeersOnly: return "Big Only"
        case .smallPeersOnly: return "Small Only"
        case .localOnly: return "Local Only"
        }
    }
}

// Extension to create Color from hex codes
extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let a, r, g, b: UInt64
        switch hex.count {
        case 3: // RGB (12-bit)
            (a, r, g, b) = (255, (int >> 8) * 17, (int >> 4 & 0xF) * 17, (int & 0xF) * 17)
        case 6: // RGB (24-bit)
            (a, r, g, b) = (255, int >> 16, int >> 8 & 0xFF, int & 0xFF)
        case 8: // ARGB (32-bit)
            (a, r, g, b) = (int >> 24, int >> 16 & 0xFF, int >> 8 & 0xFF, int & 0xFF)
        default:
            (a, r, g, b) = (1, 1, 1, 0)
        }

        self.init(
            .sRGB,
            red: Double(r) / 255,
            green: Double(g) / 255,
            blue:  Double(b) / 255,
            opacity: Double(a) / 255
        )
    }
}

