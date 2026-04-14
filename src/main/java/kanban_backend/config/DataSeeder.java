package kanban_backend.config;

import kanban_backend.model.User;
import kanban_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:admin@guvi.in}")
    private String adminEmail;

    @Value("${app.admin.password:admin123}")
    private String adminPassword;

    @Value("${app.admin.name:System Admin}")
    private String adminName;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        List<User> admins = userRepository.findByGlobalRole("ORG_ADMIN");
        if (admins.isEmpty()) {
            User admin = new User();
            admin.setName(adminName);
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setGlobalRole("ORG_ADMIN");
            admin.setActive(true);
            userRepository.save(admin);
            System.out.println("Initialized ORG_ADMIN user: " + adminEmail);
        }
    }
}
