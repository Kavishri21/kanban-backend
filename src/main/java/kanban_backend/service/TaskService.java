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
        // 1. Resolve the CREATOR (logged-in user)
        kanban_backend.model.User creator = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Creator not found"));
        
        // 2. Resolve the ASSIGNEE (from task.getUserId() sent from frontend)
        // If frontend doesn't send a userId, default to creator (self-assign)
        String assignedToId = (task.getUserId() != null && !task.getUserId().isBlank()) 
                               ? task.getUserId() : creator.getId();
        
        Instant now = Instant.now();
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        task.setStatus("todo"); 
        task.setUserId(assignedToId); // Board where task will appear
        task.setCreatedByUserId(creator.getId()); // Tracking who assigned it

        // 3. First history entry uses CREATOR's name
        task.getStatusHistory().add(new Task.StatusHistory("todo", now, creator.getName(), null));

        return taskRepository.save(task);
    }

    public Task updateStatus(String id, String newStatus, String userEmail) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found: " + id));
        kanban_backend.model.User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Instant now = Instant.now();
        task.setStatus(newStatus);
        task.setUpdatedAt(now);

        String historyReason = "backlog".equals(newStatus) ? task.getReason() : null;
        task.getStatusHistory().add(new Task.StatusHistory(newStatus, now, user.getName(), historyReason));

        return taskRepository.save(task);
    }

    public Task updateTask(String id, Task updates) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found: " + id));

        task.setTitle(updates.getTitle());
        task.setDescription(updates.getDescription());
        task.setReason(updates.getReason());
        task.setPriority(updates.getPriority());
        task.setUpdatedAt(Instant.now());

        return taskRepository.save(task);
    }

    public void deleteTask(String id) {
        taskRepository.deleteById(id);
    }
}
