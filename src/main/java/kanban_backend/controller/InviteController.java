package kanban_backend.controller;

import kanban_backend.model.Role;
import kanban_backend.service.InviteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/invites")
public class InviteController {

    private final InviteService inviteService;

    public InviteController(InviteService inviteService) {
        this.inviteService = inviteService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> createInvite(@RequestBody Map<String, String> body, Principal principal) {
        String email = body.get("email");
        Role role = Role.valueOf(body.get("role"));
        String managerId = body.get("managerId"); // The person this user will report to

        String inviteLink = inviteService.createInvite(email, role, managerId, principal.getName());
        return ResponseEntity.ok(Map.of("message", "Invite generated successfully", "inviteLink", inviteLink));
    }
}
