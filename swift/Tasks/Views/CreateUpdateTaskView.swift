import SwiftUI

/// View for creating or editing a task
struct CreateUpdateTaskView: View {
    @EnvironmentObject var listVM: TasksListScreenViewModel
    @Environment(\.dismiss) private var dismiss
    @StateObject var viewModel: EditScreenViewModel
    @FocusState var titleHasFocus: Bool

    init(task: TaskModel?) {
        self._viewModel = StateObject(
            wrappedValue: EditScreenViewModel(task: task)
        )
    }

    var body: some View {
        NavigationView {
            Form {
                Section {
                    TextField("Title", text: $viewModel.taskTitleText)
                        .focused($titleHasFocus)
                        .onSubmit(onSubmit)

                    Toggle("Is Completed", isOn: $viewModel.task.done)
                }

                if viewModel.isExistingTask {
                    Section {
                        HStack {
                            Button(
                                action: {
                                    viewModel.deleteRequested.toggle()
                                },
                                label: {
                                    Text("Delete Task")
                                        .fontWeight(.bold)
                                        .foregroundColor(
                                            viewModel.deleteRequested
                                                ? .white : .red)
                                })

                            Spacer()

                            if viewModel.deleteRequested {
                                Image(systemName: "checkmark")
                                    .foregroundColor(.white)
                            }
                        }
                        .contentShape(Rectangle())
                    }
                    .listRowBackground(
                        viewModel.deleteRequested ? Color.red : nil)
                }
            }
            .navigationTitle(
                viewModel.isExistingTask ? "Edit Task" : "Create Task"
            )
            .navigationBarItems(
                leading: Button("Cancel") {
                    dismiss()
                },
                trailing: Button(viewModel.isExistingTask ? "Save" : "Create") {
                    onSubmit()
                }
            )
        }
        .onAppear {
            if !viewModel.isExistingTask {
                titleHasFocus = true
            }
        }
    }

    func onSubmit() {
        viewModel.save(listVM: listVM)
        dismiss()
    }
}

struct EditScreen_Previews: PreviewProvider {
    static var previews: some View {
        CreateUpdateTaskView(
            task: TaskModel(title: "Get Milk", done: true)
        )
    }
}
