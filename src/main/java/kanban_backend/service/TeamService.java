package kanban_backend.service;

import kanban_backend.model.Team;
import kanban_backend.repository.TeamRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;

@Service
public class TeamService {

    private final TeamRepository teamRepository;

    public TeamService(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    public List<Team> getAllTeams() {
        return teamRepository.findAll();
    }

    private void removeUserFromAnyTeam(String userId) {
        List<Team> currentTeams = teamRepository.findByMemberIdsContaining(userId);
        for (Team t : currentTeams) {
            t.getMemberIds().remove(userId);
            teamRepository.save(t);
        }
    }

    public Team createTeam(String name, List<String> memberIds) {
        Team team = new Team();
        team.setName(name);
        
        List<String> finalMemberIds = new ArrayList<>();
        if (memberIds != null) {
            for (String userId : memberIds) {
                // Ensure exclusive membership: remove from current team before adding to new one
                removeUserFromAnyTeam(userId);
                finalMemberIds.add(userId);
            }
        }
        
        team.setMemberIds(finalMemberIds);
        return teamRepository.save(team);
    }

    public void moveMember(String userId, String toTeamId) {
        // 1. Ensure exclusive membership: remove from current team
        removeUserFromAnyTeam(userId);

        // 2. Add user to the new target team
        Team targetTeam = teamRepository.findById(toTeamId)
                .orElseThrow(() -> new RuntimeException("Target team not found"));
        
        if (!targetTeam.getMemberIds().contains(userId)) {
            targetTeam.getMemberIds().add(userId);
        }
        teamRepository.save(targetTeam);
    }
}
