package kanban_backend.controller;

import kanban_backend.model.User;
import kanban_backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;
    private final kanban_backend.service.UserService userService;

    public UserController(UserRepository userRepository, kanban_backend.service.UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    // GET /api/users — Returns all registered members (requires login)
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    // PATCH /api/users/{id}/status — Toggle active/inactive
    @PatchMapping("/{id}/status")
    public ResponseEntity<User> toggleStatus(@PathVariable String id) {
        return ResponseEntity.ok(userService.toggleUserStatus(id));
    }

    // DELETE /api/users/{id} — Delete user record
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
