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

    public Team createTeam(String name, String createdByUserId, List<String> memberIds) {
        Team team = new Team();
        team.setName(name);
        team.setCreatedByUserId(createdByUserId);
        
        List<String> finalMemberIds = new ArrayList<>();
        if (memberIds != null) {
            for (String userId : memberIds) {
                if (userId.equals(createdByUserId)) {
                    // Creator is always added — never removed from other teams
                    // (creators can span multiple teams)
                    finalMemberIds.add(userId);
                } else {
                    // Regular members: enforce one-team rule by removing from previous team first
                    removeUserFromAnyTeam(userId);
                    finalMemberIds.add(userId);
                }
            }
        }
        
        team.setMemberIds(finalMemberIds);
        return teamRepository.save(team);
    }

    public void addMembersToTeam(String teamId, List<String> memberIds) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));
        
        if (memberIds != null) {
            for (String userId : memberIds) {
                if (userId.equals(team.getCreatedByUserId())) continue; // Skip creator: they can be in multiple teams
                // Regular members: enforce one-team rule
                removeUserFromAnyTeam(userId);
                if (!team.getMemberIds().contains(userId)) {
                    team.getMemberIds().add(userId);
                }
            }
        }
        teamRepository.save(team);
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

    public Team renameTeam(String teamId, String newName) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));
        team.setName(newName);
        return teamRepository.save(team);
    }

    public void removeMemberFromTeam(String teamId, String userId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));
        team.getMemberIds().remove(userId);
        teamRepository.save(team);
    }

    public void deleteTeam(String teamId) {
        teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found"));
        teamRepository.deleteById(teamId);
    }
}
