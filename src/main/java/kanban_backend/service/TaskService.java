package kanban_backend.service;

import kanban_backend.model.Task;
import kanban_backend.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final kanban_backend.repository.UserRepository userRepository;

    public TaskService(TaskRepository taskRepository, kanban_backend.repository.UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    private String getUserId(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found")).getId();
    }

    // ----------------------------------------------------------------
    // GET all tasks
    // Phase 2: uses taskRepository.findByUserId(userId)
    // ----------------------------------------------------------------
    public List<Task> getAllTasks(String userEmail) {
        return taskRepository.findByUserId(getUserId(userEmail));
    }

    // ----------------------------------------------------------------
    // CREATE a new task
    // ----------------------------------------------------------------
    public Task createTask(Task task, String userEmail) {
        Instant now = Instant.now();
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        task.setStatus("todo"); // always starts in 'todo'
        task.setUserId(getUserId(userEmail)); // bind to specific user

        // First entry in the audit log
        task.getStatusHistory().add(new Task.StatusHistory("todo", now));

        return taskRepository.save(task);
    }

    // ----------------------------------------------------------------
    // UPDATE STATUS — called on drag-and-drop
    // This is the core feature of Phase 1:
    //   1. Changes the status field
    //   2. Updates the updatedAt timestamp to NOW
    //   3. Appends a new entry to statusHistory (full audit trail)
    // ----------------------------------------------------------------
    public Task updateStatus(String id, String newStatus) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found: " + id));

        Instant now = Instant.now();
        task.setStatus(newStatus);
        task.setUpdatedAt(now);

        // Append new entry to the audit log
        task.getStatusHistory().add(new Task.StatusHistory(newStatus, now));

        return taskRepository.save(task);
    }

    // ----------------------------------------------------------------
    // UPDATE TASK (full edit — modal save)
    // Only updates title, description, tag, priority — NOT status
    // Status should only change through updateStatus (drag-and-drop)
    // ----------------------------------------------------------------
    public Task updateTask(String id, Task updates) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found: " + id));

        task.setTitle(updates.getTitle());
        task.setDescription(updates.getDescription());
        task.setTag(updates.getTag());
        task.setPriority(updates.getPriority());
        task.setUpdatedAt(Instant.now());

        return taskRepository.save(task);
    }

    // ----------------------------------------------------------------
    // DELETE
    // ----------------------------------------------------------------
    public void deleteTask(String id) {
        taskRepository.deleteById(id);
    }
}
