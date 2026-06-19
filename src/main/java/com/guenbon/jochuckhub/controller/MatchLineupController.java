package com.guenbon.jochuckhub.controller;

import com.guenbon.jochuckhub.dto.CustomUserDetails;
import com.guenbon.jochuckhub.dto.request.SaveLineupRequest;
import com.guenbon.jochuckhub.dto.response.MatchLineupResponse;
import com.guenbon.jochuckhub.service.MatchLineupService;
import com.guenbon.jochuckhub.service.MatchLineupService.AnnounceResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

    @PutMapping
    @Operation(summary = "라인업 직접 저장 (수동 생성 / 자동 생성 후 수정)",
            description = "4쿼터 전체 라인업을 전달하면 기존 라인업을 덮어씁니다. " +
                    "각 쿼터는 정확히 10명이어야 합니다. OWNER/MANAGER만 가능합니다.")
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

    @PostMapping("/announce")
    @Operation(summary = "라인업 발표 (카카오톡 전송)",
            description = "완성된 라인업을 팀원 전체에게 카카오톡 메시지로 전송합니다. " +
                    "각 쿼터 라인업 이미지 4장이 나에게 보내기 방식으로 각 팀원에게 전달됩니다. " +
                    "OWNER/MANAGER만 가능하며, 카카오 로그인 상태인 팀원에게만 전송됩니다.")
    public ResponseEntity<MatchLineupService.AnnounceResult> announceLineup(
            @PathVariable Long matchId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(matchLineupService.announceLineup(matchId, userDetails.getMemberId()));
    }
}
