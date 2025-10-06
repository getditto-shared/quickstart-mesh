import SwiftUI

struct BulkCreateTaskView: View {
    @EnvironmentObject var listVM: TasksListScreenViewModel
    @Environment(\.dismiss) private var dismiss
    @FocusState var titleHasFocus: Bool
    @State private var taskNumberText: String = ""
    @State private var prefix: String = ""
    @State private var delay: String = ""

    var taskNumber: Int {
        return Int(taskNumberText) ?? 0
    }

    var delayInSeconds: Double {
         let milliseconds = Double(delay) ?? 0
         return milliseconds / 1000.0
     }

    var body: some View {
        NavigationView {
            Form {
                Section {
                    TextField("Number:", text: $taskNumberText)
                        .keyboardType(.numberPad)
                        .focused($titleHasFocus)
                    
                    TextField("Delay (ms):", text: $delay)
                        .keyboardType(.numberPad)
                    
                    TextField("Prefix:", text: $prefix)
                }
            }
            .navigationTitle("Create Tasks")
            .navigationBarItems(
                leading: Button("Cancel") {
                    dismiss()
                },
                trailing: Button("Create") {
                    onSubmit()
                }
                .disabled(taskNumber <= 0)
            )
        }
        .onAppear {
            titleHasFocus = true
        }
    }

    func onSubmit() {
        dismiss()

        // Run in background to avoid blocking UI
        DispatchQueue.global(qos: .userInitiated).async {
            for i in 1...self.taskNumber {

                var newTask = TaskModel()
                newTask.title = "\(self.prefix)-\(i)"
                newTask.deleted = false

                DispatchQueue.main.async {
                    self.listVM.saveNewTask(newTask)
                }

                if self.delayInSeconds > 0 {
                    Thread.sleep(forTimeInterval: self.delayInSeconds)
                }
            }
        }
    }
}
