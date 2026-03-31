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

    public List<Task> getAllTasks(String userEmail) {
        return taskRepository.findByUserId(getUserId(userEmail));
    }

    public Task createTask(Task task, String userEmail) {
        Instant now = Instant.now();
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        task.setStatus("todo"); 
        task.setUserId(getUserId(userEmail)); 

        task.getStatusHistory().add(new Task.StatusHistory("todo", now));

        return taskRepository.save(task);
    }

    public Task updateStatus(String id, String newStatus) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found: " + id));

        Instant now = Instant.now();
        task.setStatus(newStatus);
        task.setUpdatedAt(now);

        task.getStatusHistory().add(new Task.StatusHistory(newStatus, now));

        return taskRepository.save(task);
    }

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

    public void deleteTask(String id) {
        taskRepository.deleteById(id);
    }
}
