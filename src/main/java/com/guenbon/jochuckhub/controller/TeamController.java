package com.guenbon.jochuckhub.controller;

import com.guenbon.jochuckhub.dto.CustomUserDetails;
import com.guenbon.jochuckhub.dto.request.CreateTeamRequest;
import com.guenbon.jochuckhub.dto.request.CreateVirtualTeamRequest;
import com.guenbon.jochuckhub.dto.request.UpdateTeamRequest;
import com.guenbon.jochuckhub.dto.response.TeamDetailResponse;
import com.guenbon.jochuckhub.dto.response.TeamSummaryResponse;
import com.guenbon.jochuckhub.service.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @PostMapping
    public ResponseEntity<TeamDetailResponse> createTeam(
            @Valid @RequestBody CreateTeamRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(teamService.createTeam(request, userDetails));
    }

    @GetMapping
    public ResponseEntity<List<TeamSummaryResponse>> getTeams() {
        return ResponseEntity.ok(teamService.getTeams());
    }

    /**
     * 팀 이름 검색: 실제 팀 + myTeamId가 만든 가상 팀
     */
    @GetMapping("/search")
    public ResponseEntity<List<TeamSummaryResponse>> searchTeams(
            @RequestParam String name,
            @RequestParam Long myTeamId) {
        return ResponseEntity.ok(teamService.searchTeams(name, myTeamId));
    }

    @PostMapping("/virtual")
    public ResponseEntity<TeamSummaryResponse> createVirtualTeam(
            @Valid @RequestBody CreateVirtualTeamRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(teamService.createVirtualTeam(request, userDetails));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TeamDetailResponse> getTeam(@PathVariable Long id) {
        return ResponseEntity.ok(teamService.getTeam(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TeamDetailResponse> updateTeam(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTeamRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(teamService.updateTeam(id, request, userDetails));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTeam(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        teamService.deleteTeam(id, userDetails);
        return ResponseEntity.noContent().build();
    }
}
