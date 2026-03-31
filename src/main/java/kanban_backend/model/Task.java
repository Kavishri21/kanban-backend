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
    private String status;       // "todo" | "inprogress" | "done"
    private String tag;
    private String priority;     // "urgent" | "medium" | "low"

    private Instant createdAt;
    private Instant updatedAt;   // updated every time status changes via drag-and-drop

    // Full audit log of every status change with its timestamp
    private List<StatusHistory> statusHistory = new ArrayList<>();

    // --- Phase 2 hook: enabled for tokenization ---
    private String userId;

    // ---------------------------------------------------------------
    // Inner class: one entry per status change
    // ---------------------------------------------------------------
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusHistory {
        private String status;
        private Instant changedAt;
    }
}
