package kanban_backend.service;

import kanban_backend.model.Task;
import kanban_backend.model.User;
import kanban_backend.model.Counter;
import kanban_backend.repository.TaskRepository;
import kanban_backend.repository.UserRepository;
import kanban_backend.repository.TeamRepository;
import kanban_backend.repository.CounterRepository;
import kanban_backend.model.Team;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.ArrayList;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final EmailService emailService;
    private final CounterRepository counterRepository;
    private final NotificationService notificationService;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository,
                       TeamRepository teamRepository, EmailService emailService,
                       CounterRepository counterRepository, NotificationService notificationService) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.emailService = emailService;
        this.counterRepository = counterRepository;
        this.notificationService = notificationService;
    }

    /**
     * Generates a prefix code from a team name:
     *   - 2+ words: 1st letter of word 1 + first 2 letters of word 2 (uppercase)
     *   - 1 word:   first 3 letters (uppercase)
     */
    private String buildTeamPrefix(String teamName) {
        if (teamName == null || teamName.isBlank()) return "MY";
        String[] words = teamName.trim().toUpperCase().split("\\s+");
        if (words.length == 1) {
            String w = words[0].replaceAll("[^A-Z]", "");
            return w.length() >= 3 ? w.substring(0, 3) : w;
        }
        String w1 = words[0].replaceAll("[^A-Z]", "");
        String w2 = words[1].replaceAll("[^A-Z]", "");
        String p1 = w1.isEmpty() ? "" : w1.substring(0, 1);
        String p2 = w2.length() >= 2 ? w2.substring(0, 2) : w2;
        return p1 + p2;
    }

    /**
     * Atomically increment and return the next sequence number for a given prefix and context.
     */
    private synchronized long getNextSequence(String prefix, String contextId) {
        String counterId = prefix + ":" + contextId;
        Counter counter = counterRepository.findById(counterId)
                .orElse(new Counter(counterId, prefix, contextId, 0L));
        counter.setSeq(counter.getSeq() + 1);
        counterRepository.save(counter);
        return counter.getSeq();
    }

    public List<Task> getTasks(String userEmail, String teamId, boolean createdByMe) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
                
        boolean isOrgAdmin = "ORG_ADMIN".equals(user.getGlobalRole());
        
        List<Task> allTasks;
        if (teamId == null || teamId.isBlank()) {
            if (isOrgAdmin) {
                if (createdByMe) {
                    allTasks = taskRepository.findAll().stream()
                        .filter(t -> user.getId().equals(t.getCreatedByUserId()))
                        .collect(Collectors.toList());
                } else {
                    allTasks = taskRepository.findAll();
                }
            } else {
                // If no team specified, maybe finding tasks across all teams they are part of
                // For safety, let's just return tasks they own or created
                // Need to also combine tasks they created. MongoDB has no simple OR in MongoRepository without @Query unless we write one. Let's filter in memory if small, or just wait.
                // It's better to expect teamId.
                allTasks = taskRepository.findAll().stream()
                    .filter(t -> {
                        if (createdByMe) return user.getId().equals(t.getCreatedByUserId());
                        return user.getId().equals(t.getUserId()) || user.getId().equals(t.getCreatedByUserId());
                    })
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
                if (createdByMe) {
                    allTasks = teamTasks.stream()
                        .filter(t -> user.getId().equals(t.getCreatedByUserId()))
                        .collect(Collectors.toList());
                } else {
                    allTasks = teamTasks;
                }
            } else {
                // The explicit user requirement: "should see only tasks that are creataed by him or assigned to him from higher level members and not the entire team tasks"
                allTasks = teamTasks.stream()
                    .filter(t -> {
                        if (createdByMe) return user.getId().equals(t.getCreatedByUserId());
                        return user.getId().equals(t.getUserId()) || user.getId().equals(t.getCreatedByUserId());
                    })
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

        // ── Generate Human-Readable Task ID ─────────────────────────────────
        String prefix;
        String contextId;
        if (task.getTeamId() != null && !task.getTeamId().isBlank()) {
            Team teamForId = teamRepository.findById(task.getTeamId()).orElse(null);
            prefix = (teamForId != null) ? buildTeamPrefix(teamForId.getName()) : "MY";
            contextId = task.getTeamId();
        } else {
            prefix = "MY";
            contextId = creator.getId();
        }
        long seq = getNextSequence(prefix, contextId);
        task.setTaskID(prefix + seq);
        // ────────────────────────────────────────────────────────────────────

        Task savedTask = taskRepository.save(task);

        // ── Email Notification ─────────────────────────────────────────────
        // Only notify if assigner != assignee (skip self-assignments like "Myself")
        if (!creator.getId().equals(assignedToId)) {
            try {
                User assignee = userRepository.findById(assignedToId)
                    .orElseThrow(() -> new RuntimeException("Assignee not found"));

                String teamName = "Personal Task";
                if (task.getTeamId() != null && !task.getTeamId().isBlank()) {
                    teamName = teamRepository.findById(task.getTeamId())
                        .map(t -> t.getName())
                        .orElse("Unknown Team");
                }

                emailService.sendTaskAssignmentEmail(
                    assignee.getEmail(),
                    assignee.getName(),
                    creator.getName(),
                    task.getTitle(),
                    task.getPriority() != null ? task.getPriority() : "medium",
                    teamName
                );
                
                // --- In-App Notification ---
                notificationService.createNotification(
                    assignee.getId(),
                    creator.getId(),
                    creator.getName(),
                    "TASK_ASSIGNED",
                    "assigned you a new task: " + task.getTitle(),
                    savedTask.getId()
                );
            } catch (Exception e) {
                // Email is optional — task is already saved, just log the error
                System.err.println("Could not send task assignment email: " + e.getMessage());
            }
        }
        // ────────────────────────────────────────────────────────────────────

        return savedTask;
    }

    public Task updateStatus(String id, String newStatus, String userEmail) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found: " + id));
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!"ORG_ADMIN".equals(user.getGlobalRole())) {
            if (task.getTeamId() != null && !task.getTeamId().isBlank()) {
                // Team task — verify user is a member of that team
                Team team = teamRepository.findById(task.getTeamId())
                        .orElseThrow(() -> new RuntimeException("Team not found"));
                boolean isMember = team.getMembers().stream()
                        .anyMatch(m -> m.getUserId().equals(user.getId()));
                if (!isMember) {
                    throw new RuntimeException("Access denied: not a team member.");
                }
            } else {
                // Personal task (no teamId) — only the owner or creator can move it
                boolean isOwnerOrCreator = user.getId().equals(task.getUserId())
                        || user.getId().equals(task.getCreatedByUserId());
                if (!isOwnerOrCreator) {
                    throw new RuntimeException("Access denied: not the task owner.");
                }
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
        task.setDueDate(updates.getDueDate());
        task.setUpdatedAt(Instant.now());

        return taskRepository.save(task);
    }

    public void deleteTask(String id) {
        taskRepository.deleteById(id);
    }

    public Task addComment(String taskId, Task.Comment comment, String userEmail) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        User author = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        comment.setId(UUID.randomUUID().toString());
        comment.setCreatedAt(Instant.now());
        comment.setAuthorId(author.getId());
        comment.setAuthorName(author.getName());
        
        if (comment.getMentionedUserIds() == null) {
            comment.setMentionedUserIds(new ArrayList<>());
        }
        if (comment.getReadBy() == null) {
            comment.setReadBy(new ArrayList<>());
        }
        
        // Add author to readBy so they don't see their own comment as unread
        comment.getReadBy().add(author.getId());

        task.getComments().add(comment);
        task.setUpdatedAt(Instant.now());
        Task savedTask = taskRepository.save(task);

        // --- Send Notifications for Mentions ---
        if (comment.getMentionedUserIds() != null && !comment.getMentionedUserIds().isEmpty()) {
            for (String mentionedUserId : comment.getMentionedUserIds()) {
                notificationService.createNotification(
                    mentionedUserId,
                    author.getId(),
                    author.getName(),
                    "MENTION",
                    "mentioned you in a comment on task: " + task.getTitle(),
                    savedTask.getId()
                );
            }
        }

        return savedTask;
    }

    public Task markCommentAsRead(String taskId, String commentId, String userEmail) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        task.getComments().forEach(c -> {
            if (c.getId().equals(commentId)) {
                if (!c.getReadBy().contains(user.getId())) {
                    c.getReadBy().add(user.getId());
                }
            }
        });

        return taskRepository.save(task);
    }

    public Task updateComment(String taskId, String commentId, Task.Comment updates, String userEmail) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        task.getComments().forEach(c -> {
            if (c.getId().equals(commentId)) {
                if (!c.getAuthorId().equals(user.getId())) {
                    throw new RuntimeException("Unauthorized: You can only edit your own comments");
                }
                c.setText(updates.getText());
                c.setMentionedUserIds(updates.getMentionedUserIds());
            }
        });

        task.setUpdatedAt(Instant.now());
        return taskRepository.save(task);
    }

    public Task deleteComment(String taskId, String commentId, String userEmail) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean removed = task.getComments().removeIf(c -> {
            if (c.getId().equals(commentId)) {
                if (!c.getAuthorId().equals(user.getId())) {
                    throw new RuntimeException("Unauthorized: You can only delete your own comments");
                }
                return true;
            }
            return false;
        });

        if (removed) {
            task.setUpdatedAt(Instant.now());
            return taskRepository.save(task);
        }
        return task;
    }
}
