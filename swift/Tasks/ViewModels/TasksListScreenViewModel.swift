import Combine
import DittoSwift
import SwiftUI

/// View model for TasksListScreen
@MainActor
class TasksListScreenViewModel: ObservableObject {
    @Published var tasks = [TaskModel]()
    @Published var count: Int = 0
    @Published var isPresentingEditScreen: Bool = false
    @Published var isPresentingBulkAddScreen: Bool = false
    @Published var isPresentingConfigurationView: Bool = false
    private(set) var taskToEdit: TaskModel?

    private let ditto = DittoManager.shared.ditto
    private var subscription: DittoSyncSubscription?
    private var taskObserver: DittoStoreObserver?
    private var countObserver: DittoStoreObserver?


    init() {
        populateTasksCollection()

        startTaskObserver()
        startCountObserver()
    }

    deinit {
        subscription?.cancel()
        subscription = nil

        taskObserver?.cancel()
        taskObserver = nil

        countObserver?.cancel()
        countObserver = nil

        if ditto.sync.isActive {
            ditto.sync.stop()
        }
    }
    
    func toggleBluetoothLE() {
        ditto.transportConfig.peerToPeer.bluetoothLE.isEnabled.toggle()
    }
    
    func toggleLAN() {
        ditto.transportConfig.peerToPeer.lan.isEnabled.toggle()
    }
    
    func toggleAWDL() {
        ditto.transportConfig.peerToPeer.awdl.isEnabled.toggle()
    }

    func setSyncEnabled(_ newValue: Bool) throws {
        if !ditto.sync.isActive && newValue {
            try startSync()
        } else if ditto.sync.isActive && !newValue {
            stopSync()
        }
    }

    private func startSync() throws {
        let query = "SELECT * from tasks"
        
        try ditto.sync.start()
        subscription = try ditto.sync.registerSubscription(query: query)
    }

    private func stopSync() {
        subscription?.cancel()
        subscription = nil

        ditto.sync.stop()
    }
    
    private func startTaskObserver() {
        let query = "SELECT * FROM tasks WHERE NOT deleted ORDER BY title ASC LIMIT 50"
        
        taskObserver = try? ditto.store.registerObserver(query: query) {
            [weak self] result in
            guard let self = self else { return }
            self.tasks = result.items.compactMap {
                TaskModel($0.jsonData())
            }
        }
    }
    
    private func startCountObserver() {
        let query = "SELECT COUNT(*) as result FROM tasks where NOT deleted"
        
        countObserver = try? ditto.store.registerObserver(query: query) {
            [weak self] result in
            guard let self = self else { return }

            self.count = (result.items.first?.value as? [String: Any?])?["result"] as? Int ?? 0
        }
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
    
    nonisolated func deleteAllTasks() {
        Task {
            let query = "UPDATE tasks SET deleted = true WHERE done = false"
            do {
                try await ditto.store.execute(query: query)
            } catch {
                print(
                    "TaskListScreenVM.\(#function) - ERROR deleting tasks: \(error.localizedDescription)"
                )
            }
        }
    }
    
    nonisolated func evictAllDeletedTasks() {
        Task {
            let query = "EVICT FROM tasks WHERE deleted = true"
            do {
                try await ditto.store.execute(query: query)
            } catch {
                print(
                    "TaskListScreenVM.\(#function) - ERROR evicting tasks: \(error.localizedDescription)"
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
}
