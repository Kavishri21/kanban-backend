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

    private void verifyOrgAdmin(String requesterEmail) {
        User requester = userRepository.findByEmail(requesterEmail).orElseThrow(() -> new RuntimeException("Requester not found"));
        if (!"ORG_ADMIN".equals(requester.getGlobalRole())) {
            throw new RuntimeException("Access Denied: Only ORG_ADMIN can perform this action.");
        }
    }

    public User updateGlobalRole(String id, String newRole, String requesterEmail) {
        verifyOrgAdmin(requesterEmail);
        
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        
        // Prevent removing the last ORG_ADMIN
        if ("ORG_ADMIN".equals(user.getGlobalRole()) && !"ORG_ADMIN".equals(newRole)) {
            long adminCount = userRepository.findByGlobalRole("ORG_ADMIN").size();
            if (adminCount <= 1) {
                throw new RuntimeException("Cannot demote the last ORG_ADMIN. Promote another user first.");
            }
        }
        
        user.setGlobalRole(newRole);
        return userRepository.save(user);
    }

    public User toggleUserStatus(String id, String requesterEmail) {
        verifyOrgAdmin(requesterEmail);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setActive(!user.isActive());
        return userRepository.save(user);
    }

    public void deleteUser(String id, String requesterEmail) {
        verifyOrgAdmin(requesterEmail);
        
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
        
        // Prevent deleting the last ORG_ADMIN
        if ("ORG_ADMIN".equals(user.getGlobalRole())) {
            long adminCount = userRepository.findByGlobalRole("ORG_ADMIN").size();
            if (adminCount <= 1) {
                throw new RuntimeException("Cannot delete the last ORG_ADMIN.");
            }
        }
        
        // Unassign all tasks from this user before deleting them
        List<Task> userTasks = taskRepository.findByUserId(id);
        for (Task task : userTasks) {
            task.setUserId(null); // Mark as unassigned
        }
        taskRepository.saveAll(userTasks);

        userRepository.deleteById(id);
    }
}
