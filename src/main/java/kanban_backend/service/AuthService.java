package kanban_backend.service;

import kanban_backend.model.User;
import kanban_backend.repository.UserRepository;
import kanban_backend.security.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    private final kanban_backend.repository.InviteRepository inviteRepository;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil, AuthenticationManager authenticationManager,
                       kanban_backend.repository.InviteRepository inviteRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.inviteRepository = inviteRepository;
    }

    public Map<String, Object> register(Map<String, String> request) {
        String token = request.get("token");
        if (token == null || token.isBlank()) {
            throw new RuntimeException("Signup requires an invite token.");
        }

        kanban_backend.model.Invite invite = inviteRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid or expired invite token."));

        if ("ACCEPTED".equals(invite.getStatus())) {
            throw new RuntimeException("This invite has already been used.");
        }

        if (java.time.Instant.now().isAfter(invite.getExpiry())) {
            throw new RuntimeException("This invite has expired.");
        }

        Optional<User> existingUser = userRepository.findByEmail(invite.getEmail());
        if (existingUser.isPresent()) {
            throw new RuntimeException("Email already in use.");
        }

        User user = new User();
        user.setName(request.get("name"));
        user.setEmail(invite.getEmail()); // Forced from invite
        user.setPassword(passwordEncoder.encode(request.get("password")));
        user.setRole(invite.getRole()); // Forced from invite
        user.setManagerId(invite.getManagerId()); // Forced from invite
        
        userRepository.save(user);

        invite.setStatus("ACCEPTED");
        inviteRepository.save(invite);

        String jwtToken = jwtUtil.generateToken(user.getEmail());

        Map<String, Object> response = new HashMap<>();
        response.put("token", jwtToken);
        response.put("user", user); 
        return response;
    }

    public Map<String, Object> login(User request) {
        // Authenticate credentials
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtUtil.generateToken(user.getEmail());

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("user", user);
        return response;
    }
}
