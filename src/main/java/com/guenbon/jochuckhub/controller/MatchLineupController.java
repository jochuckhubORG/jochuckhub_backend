package com.guenbon.jochuckhub.controller;

import com.guenbon.jochuckhub.dto.CustomUserDetails;
import com.guenbon.jochuckhub.dto.request.SaveLineupRequest;
import com.guenbon.jochuckhub.dto.response.MatchLineupResponse;
import com.guenbon.jochuckhub.service.MatchLineupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/matches/{matchId}/lineup")
@RequiredArgsConstructor
@Tag(name = "Match Lineup", description = "매치 라인업 API")
public class MatchLineupController {

    private final MatchLineupService matchLineupService;

    @PostMapping
    @Operation(summary = "라인업 자동 생성")
    public ResponseEntity<MatchLineupResponse> generateLineup(
            @PathVariable Long matchId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(matchLineupService.generateLineup(matchId, userDetails.getMemberId()));
    }

    @PutMapping
    @Operation(summary = "라인업 수동 저장")
    public ResponseEntity<MatchLineupResponse> saveLineup(
            @PathVariable Long matchId,
            @RequestBody @Valid SaveLineupRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(matchLineupService.saveLineup(matchId, request, userDetails.getMemberId()));
    }

    @GetMapping
    @Operation(summary = "라인업 조회")
    public ResponseEntity<MatchLineupResponse> getLineup(@PathVariable Long matchId) {
        return ResponseEntity.ok(matchLineupService.getLineup(matchId));
    }
}
