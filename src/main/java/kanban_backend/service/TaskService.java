package kanban_backend.service;

import kanban_backend.model.Task;
import kanban_backend.model.User;
import kanban_backend.repository.TaskRepository;
import kanban_backend.repository.UserRepository;
import kanban_backend.repository.TeamRepository;
import kanban_backend.model.Team;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository, TeamRepository teamRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
    }

    public List<Task> getTasks(String userEmail, String teamId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
                
        boolean isOrgAdmin = "ORG_ADMIN".equals(user.getGlobalRole());
        
        List<Task> allTasks;
        if (teamId == null || teamId.isBlank()) {
            if (isOrgAdmin) {
                allTasks = taskRepository.findAll();
            } else {
                // If no team specified, maybe finding tasks across all teams they are part of
                // For safety, let's just return tasks they own or created
                // Need to also combine tasks they created. MongoDB has no simple OR in MongoRepository without @Query unless we write one. Let's filter in memory if small, or just wait.
                // It's better to expect teamId.
                allTasks = taskRepository.findAll().stream()
                    .filter(t -> user.getId().equals(t.getUserId()) || user.getId().equals(t.getCreatedByUserId()))
                    .collect(Collectors.toList());
            }
        } else {
            // Verify team membership
            if (!isOrgAdmin) {
                Team team = teamRepository.findById(teamId).orElseThrow(() -> new RuntimeException("Team not found"));
                boolean isMember = team.getMembers().stream().anyMatch(m -> m.getUserId().equals(user.getId()));
                if (!isMember) {
                    throw new RuntimeException("You do not have access to this team's tasks");
                }
            }
            
            List<Task> teamTasks = taskRepository.findAll().stream()
                .filter(t -> teamId.equals(t.getTeamId()))
                .collect(Collectors.toList());
                
            if (isOrgAdmin) {
                allTasks = teamTasks;
            } else {
                // The explicit user requirement: "should see only tasks that are creataed by him or assigned to him from higher level members and not the entire team tasks"
                allTasks = teamTasks.stream()
                    .filter(t -> user.getId().equals(t.getUserId()) || user.getId().equals(t.getCreatedByUserId()))
                    .collect(Collectors.toList());
            }
        }
        
        return allTasks;
    }

    public Task createTask(Task task, String userEmail) {
        User creator = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Creator not found"));
        
        // Verify creator is in the team if a team is specified
        if (task.getTeamId() != null && !task.getTeamId().isBlank()) {
            boolean isOrgAdmin = "ORG_ADMIN".equals(creator.getGlobalRole());
            if (!isOrgAdmin) {
                Team team = teamRepository.findById(task.getTeamId()).orElseThrow(() -> new RuntimeException("Team not found"));
                boolean isMember = team.getMembers().stream().anyMatch(m -> m.getUserId().equals(creator.getId()));
                if (!isMember) {
                     throw new RuntimeException("You cannot create tasks for a team you are not a member of.");
                }
            }
        }

        String assignedToId = (task.getUserId() != null && !task.getUserId().isBlank()) 
                               ? task.getUserId() : creator.getId();
        
        Instant now = Instant.now();
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        task.setStatus("todo"); 
        task.setUserId(assignedToId); 
        task.setCreatedByUserId(creator.getId());

        task.getStatusHistory().add(new Task.StatusHistory("todo", now, creator.getName(), null));

        return taskRepository.save(task);
    }

    public Task updateStatus(String id, String newStatus, String userEmail) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found: " + id));
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!"ORG_ADMIN".equals(user.getGlobalRole())) {
             Team team = teamRepository.findById(task.getTeamId()).orElseThrow(() -> new RuntimeException("Team not found"));
             boolean isMember = team.getMembers().stream().anyMatch(m -> m.getUserId().equals(user.getId()));
             if (!isMember) {
                 throw new RuntimeException("Access denied.");
             }
        }

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
