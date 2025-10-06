import Combine
import SwiftUI

/// View model for EditScreen
class EditScreenViewModel: ObservableObject {
    @Published var taskTitleText: String
    @Published var isExistingTask: Bool = false
    @Published var deleteRequested = false
    @Published var task: TaskModel

    init(task: TaskModel?) {
        self.task = task ?? TaskModel()
        self.taskTitleText = task?.title ?? ""
        isExistingTask = task != nil
    }

    func save(listVM: TasksListScreenViewModel) {
        if isExistingTask {
            task.title = taskTitleText
            task.deleted = deleteRequested
            listVM.saveEditedTask(task)
        } else {
            task.title = taskTitleText
            listVM.saveNewTask(task)
        }
    }
}
