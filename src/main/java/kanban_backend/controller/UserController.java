package kanban_backend.controller;

import kanban_backend.model.User;
import kanban_backend.repository.UserRepository;
import kanban_backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    public UserController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @GetMapping("/my-team")
    public ResponseEntity<List<User>> getMyTeam(Principal principal) {
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<User> subordinates = userService.getAllSubordinates(user.getId());
        return ResponseEntity.ok(subordinates);
    }
}
