package kanban_backend.controller;

import kanban_backend.model.Task;
import kanban_backend.service.TaskService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.security.Principal;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    // ----------------------------------------------------------------
    // GET /api/tasks  — fetch all tasks (load the board)
    // Phase 2: extract userId from JWT token and filter by user
    // ----------------------------------------------------------------
    @GetMapping
    public ResponseEntity<List<Task>> getAllTasks(Principal principal) {
        return ResponseEntity.ok(taskService.getAllTasks(principal.getName()));
    }

    // ----------------------------------------------------------------
    // POST /api/tasks  — create a new task
    // ----------------------------------------------------------------
    @PostMapping
    public ResponseEntity<Task> createTask(@RequestBody Task task, Principal principal) {
        Task created = taskService.createTask(task, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ----------------------------------------------------------------
    // PATCH /api/tasks/{id}/status  — drag-and-drop status update
    // Body: { "status": "inprogress" }
    // Updates status + timestamp + appends to statusHistory in MongoDB
    // ----------------------------------------------------------------
    @PatchMapping("/{id}/status")
    public ResponseEntity<Task> updateStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {

        String newStatus = body.get("status");
        if (newStatus == null || newStatus.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        Task updated = taskService.updateStatus(id, newStatus);
        return ResponseEntity.ok(updated);
    }

    // ----------------------------------------------------------------
    // PUT /api/tasks/{id}  — full task edit (modal save)
    // Updates title, description, tag, priority
    // ----------------------------------------------------------------
    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(
            @PathVariable String id,
            @RequestBody Task task) {

        Task updated = taskService.updateTask(id, task);
        return ResponseEntity.ok(updated);
    }

    // ----------------------------------------------------------------
    // DELETE /api/tasks/{id}  — delete a task
    // ----------------------------------------------------------------
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable String id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}
