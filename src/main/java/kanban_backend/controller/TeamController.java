package kanban_backend.controller;

import kanban_backend.model.Team;
import kanban_backend.service.TeamService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping
    public ResponseEntity<List<Team>> getAllTeams() {
        return ResponseEntity.ok(teamService.getAllTeams());
    }

    @PostMapping
    @SuppressWarnings("unchecked")
    public ResponseEntity<Team> createTeam(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        String createdByUserId = (String) body.get("createdByUserId");
        List<String> memberIds = (List<String>) body.get("memberIds");
        Team team = teamService.createTeam(name, createdByUserId, memberIds);
        return ResponseEntity.status(HttpStatus.CREATED).body(team);
    }

    @PatchMapping("/{teamId}/members")
    public ResponseEntity<Void> addMembers(
            @PathVariable String teamId,
            @RequestBody Map<String, List<String>> body) {
        List<String> memberIds = body.get("memberIds");
        teamService.addMembersToTeam(teamId, memberIds);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/members/{userId}/move")
    public ResponseEntity<Void> moveMember(
            @PathVariable String userId,
            @RequestParam String toTeamId) {
        teamService.moveMember(userId, toTeamId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{teamId}/name")
    public ResponseEntity<Team> renameTeam(
            @PathVariable String teamId,
            @RequestBody Map<String, String> body) {
        String newName = body.get("name");
        return ResponseEntity.ok(teamService.renameTeam(teamId, newName));
    }

    @DeleteMapping("/{teamId}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable String teamId,
            @PathVariable String userId) {
        teamService.removeMemberFromTeam(teamId, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{teamId}")
    public ResponseEntity<Void> deleteTeam(@PathVariable String teamId) {
        teamService.deleteTeam(teamId);
        return ResponseEntity.ok().build();
    }
}
