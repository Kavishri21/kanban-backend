package kanban_backend.repository;

import kanban_backend.model.Task;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends MongoRepository<Task, String> {
    // Spring Data MongoDB auto-implements all CRUD methods

    // Phase 2 hook: enabled
    java.util.List<Task> findByUserId(String userId);
}
