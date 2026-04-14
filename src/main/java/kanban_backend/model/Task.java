package kanban_backend.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "tasks")
public class Task {

    @Id
    private String id;

    private String title;
    private String description;
    private String status;       
    private String reason;
    private String priority;     

    private Instant createdAt;
    private Instant updatedAt;   

    private List<StatusHistory> statusHistory = new ArrayList<>();

    private String teamId;          // The team this task belongs to
    private String userId;          // The assigned user
    private String createdByUserId; // Who created/assigned this task


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusHistory {
        private String status;
        private Instant changedAt;
        private String changedBy;
        private String reason;
    }
}
