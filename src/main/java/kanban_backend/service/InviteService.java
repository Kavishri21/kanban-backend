package kanban_backend.service;

import kanban_backend.model.Invite;
import kanban_backend.model.Role;
import kanban_backend.model.User;
import kanban_backend.repository.InviteRepository;
import kanban_backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class InviteService {

    private final InviteRepository inviteRepository;
    private final UserRepository userRepository;

    public InviteService(InviteRepository inviteRepository, UserRepository userRepository) {
        this.inviteRepository = inviteRepository;
        this.userRepository = userRepository;
    }

    public String createInvite(String email, Role role, String managerId, String assignerEmail) {
        User assigner = userRepository.findByEmail(assignerEmail)
                .orElseThrow(() -> new RuntimeException("Assigner not found"));

        if (assigner.getRole() == Role.EMPLOYEE || assigner.getRole() == Role.INTERN) {
            throw new RuntimeException("You do not have permission to invite users.");
        }

        Invite invite = new Invite();
        invite.setEmail(email);
        invite.setRole(role);
        invite.setManagerId(managerId);
        invite.setToken(UUID.randomUUID().toString());
        invite.setStatus("PENDING");
        invite.setExpiry(Instant.now().plus(7, ChronoUnit.DAYS)); // 7 days expiry

        inviteRepository.save(invite);

        // For now, we mock the email by returning the link directly in the API response
        return "http://localhost:5173/auth?token=" + invite.getToken(); 
    }
}
