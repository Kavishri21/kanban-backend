package kanban_backend;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
public class TestBcrypt {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        System.out.println("TEST MATCH = " + encoder.matches("admin123", "$2a$10$wTf2W/Z.iWJwM478I0x.yOH.QEv5Dpxn6U8x76UvX7z8Q7jNXXu1i"));
    }
}
