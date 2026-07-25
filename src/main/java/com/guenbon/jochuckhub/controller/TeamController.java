package com.guenbon.jochuckhub.controller;

import com.guenbon.jochuckhub.dto.CustomUserDetails;
import com.guenbon.jochuckhub.dto.request.CreateTeamRequest;
import com.guenbon.jochuckhub.dto.request.CreateVirtualTeamRequest;
import com.guenbon.jochuckhub.dto.request.ProcessJoinRequest;
import com.guenbon.jochuckhub.dto.request.UpdateTeamRequest;
import com.guenbon.jochuckhub.dto.response.TeamDetailResponse;
import com.guenbon.jochuckhub.dto.response.TeamJoinRequestResponse;
import com.guenbon.jochuckhub.dto.response.TeamMemberStatsResponse;
import com.guenbon.jochuckhub.dto.response.TeamSummaryResponse;
import com.guenbon.jochuckhub.service.StatsService;
import com.guenbon.jochuckhub.service.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;
    private final StatsService statsService;

    @PostMapping
    public ResponseEntity<TeamDetailResponse> createTeam(
            @Valid @RequestBody CreateTeamRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(teamService.createTeam(request, userDetails));
    }

    @GetMapping
    public ResponseEntity<List<TeamSummaryResponse>> getTeams(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(teamService.getTeams(userDetails.getMemberId()));
    }

    @GetMapping("/search")
    public ResponseEntity<List<TeamSummaryResponse>> searchTeams(
            @RequestParam String name,
            @RequestParam(required = false) Long myTeamId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(teamService.searchTeams(name, myTeamId, userDetails.getMemberId()));
    }

    @PostMapping("/virtual")
    public ResponseEntity<TeamSummaryResponse> createVirtualTeam(
            @Valid @RequestBody CreateVirtualTeamRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(teamService.createVirtualTeam(request, userDetails));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TeamDetailResponse> getTeam(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(teamService.getTeam(id, userDetails.getMemberId()));
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<TeamJoinRequestResponse> requestToJoinTeam(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(teamService.requestToJoinTeam(id, userDetails.getMemberId()));
    }

    @GetMapping("/{id}/join-requests")
    public ResponseEntity<List<TeamJoinRequestResponse>> getPendingJoinRequests(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(teamService.getPendingJoinRequests(id, userDetails.getMemberId()));
    }

    @PatchMapping("/{id}/join-requests/{requestId}")
    public ResponseEntity<TeamJoinRequestResponse> processJoinRequest(
            @PathVariable Long id,
            @PathVariable Long requestId,
            @Valid @RequestBody ProcessJoinRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(teamService.processJoinRequest(id, requestId, request, userDetails.getMemberId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TeamDetailResponse> updateTeam(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTeamRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(teamService.updateTeam(id, request, userDetails));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<List<TeamMemberStatsResponse>> getTeamMembers(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(statsService.getTeamMemberStats(id, userDetails.getMemberId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTeam(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        teamService.deleteTeam(id, userDetails);
        return ResponseEntity.noContent().build();
    }
}
