package kanban_backend.service;

import kanban_backend.model.Task;
import kanban_backend.model.User;
import kanban_backend.repository.TaskRepository;
import kanban_backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final TaskRepository taskRepository;

    public UserService(UserRepository userRepository, TaskRepository taskRepository) {
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
    }

    public User toggleUserStatus(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setActive(!user.isActive());
        return userRepository.save(user);
    }

    public void deleteUser(String id) {
        userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        
        // Unassign all tasks from this user before deleting them
        List<Task> userTasks = taskRepository.findByUserId(id);
        for (Task task : userTasks) {
            task.setUserId(null); // Mark as unassigned
        }
        taskRepository.saveAll(userTasks);

        // 2. Delete the user
        userRepository.deleteById(id);
    }
}
