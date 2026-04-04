package kanban_backend.repository;

import kanban_backend.model.Invite;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InviteRepository extends MongoRepository<Invite, String> {
    Optional<Invite> findByToken(String token);
}
