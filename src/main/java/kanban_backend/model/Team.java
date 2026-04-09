package kanban_backend.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "teams")
@Data
@NoArgsConstructor
public class Team {
    @Id
    private String id;
    private String name;
    private List<String> memberIds = new ArrayList<>();
}
