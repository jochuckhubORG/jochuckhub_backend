package com.guenbon.jochuckhub.controller;

import com.guenbon.jochuckhub.dto.CustomUserDetails;
import com.guenbon.jochuckhub.dto.response.MatchLineupResponse;
import com.guenbon.jochuckhub.service.MatchLineupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/matches/{matchId}/lineup")
@RequiredArgsConstructor
@Tag(name = "Match Lineup", description = "매치 라인업 자동 생성 API")
public class MatchLineupController {

    private final MatchLineupService matchLineupService;

    @PostMapping
    @Operation(summary = "라인업 자동 생성",
            description = "투표 마감 후 OWNER/MANAGER가 출석율 점수와 포지션을 기반으로 4쿼터 라인업을 자동 생성합니다. " +
                    "참석 인원은 14~20명이어야 합니다. 기존 라인업이 있으면 덮어씁니다.")
    public ResponseEntity<MatchLineupResponse> generateLineup(
            @PathVariable Long matchId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(matchLineupService.generateLineup(matchId, userDetails.getMemberId()));
    }

    @GetMapping
    @Operation(summary = "라인업 조회")
    public ResponseEntity<MatchLineupResponse> getLineup(@PathVariable Long matchId) {
        return ResponseEntity.ok(matchLineupService.getLineup(matchId));
    }
}
