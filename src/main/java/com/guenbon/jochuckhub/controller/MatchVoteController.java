package com.guenbon.jochuckhub.controller;

import com.guenbon.jochuckhub.dto.CustomUserDetails;
import com.guenbon.jochuckhub.dto.request.MatchVoteRequest;
import com.guenbon.jochuckhub.dto.request.UpdateActualStatusRequest;
import com.guenbon.jochuckhub.dto.response.MatchVoteResponse;
import com.guenbon.jochuckhub.dto.response.MatchVoteResultResponse;
import com.guenbon.jochuckhub.service.MatchVoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/matches/{matchId}/votes")
@RequiredArgsConstructor
public class MatchVoteController {

    private final MatchVoteService matchVoteService;

    @PostMapping
    public ResponseEntity<MatchVoteResponse> vote(
            @PathVariable Long matchId,
            @Valid @RequestBody MatchVoteRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(matchVoteService.vote(matchId, request, userDetails));
    }

    @PutMapping
    public ResponseEntity<MatchVoteResponse> updateVote(
            @PathVariable Long matchId,
            @Valid @RequestBody MatchVoteRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(matchVoteService.updateVote(matchId, request, userDetails));
    }

    @GetMapping
    public ResponseEntity<MatchVoteResultResponse> getVoteResult(
            @PathVariable Long matchId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(matchVoteService.getVoteResult(matchId, userDetails));
    }

    // 매치 시작 후 OWNER/MANAGER가 참석자의 실제 출석 상태(지각/무단불참) 표시
    @PatchMapping("/{memberId}/actual-status")
    public ResponseEntity<MatchVoteResponse> updateActualStatus(
            @PathVariable Long matchId,
            @PathVariable Long memberId,
            @RequestBody UpdateActualStatusRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(matchVoteService.updateActualStatus(matchId, memberId, request, userDetails));
    }
}
