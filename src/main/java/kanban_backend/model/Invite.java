package kanban_backend.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "invites")
public class Invite {

    @Id
    private String id;

    private String email;
    private Role role;
    private String managerId;
    
    private String token;
    private String status; // PENDING or ACCEPTED
    private Instant expiry;
}
