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
    public ResponseEntity<Team> createTeam(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        List<String> memberIds = (List<String>) body.get("memberIds");
        Team team = teamService.createTeam(name, memberIds);
        return ResponseEntity.status(HttpStatus.CREATED).body(team);
    }

    @PatchMapping("/members/{userId}/move")
    public ResponseEntity<Void> moveMember(
            @PathVariable String userId,
            @RequestParam String toTeamId) {
        teamService.moveMember(userId, toTeamId);
        return ResponseEntity.ok().build();
    }
}
