package kanban_backend.repository;

import kanban_backend.model.Team;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface TeamRepository extends MongoRepository<Team, String> {
    // Find all teams that contain a specific userId in their memberIds list
    List<Team> findByMemberIdsContaining(String userId);
}
