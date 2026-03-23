package com.guenbon.jochuckhub.controller;

import com.guenbon.jochuckhub.dto.CustomUserDetails;
import com.guenbon.jochuckhub.dto.request.CreateMatchRequest;
import com.guenbon.jochuckhub.dto.request.RecordMatchResultRequest;
import com.guenbon.jochuckhub.dto.response.MatchResponse;
import com.guenbon.jochuckhub.dto.response.MatchResultResponse;
import com.guenbon.jochuckhub.service.MatchResultService;
import com.guenbon.jochuckhub.service.MatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;
    private final MatchResultService matchResultService;

    @PostMapping
    public ResponseEntity<MatchResponse> createMatch(
            @Valid @RequestBody CreateMatchRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(matchService.createMatch(request, userDetails));
    }

    @GetMapping
    public ResponseEntity<List<MatchResponse>> getMatchesByTeam(@RequestParam Long teamId) {
        return ResponseEntity.ok(matchService.getMatchesByTeam(teamId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MatchResponse> getMatch(@PathVariable Long id) {
        return ResponseEntity.ok(matchService.getMatch(id));
    }

    // 매치 종료 후 결과 입력 (최초 입력 및 수정 모두 동일 엔드포인트)
    @PutMapping("/{id}/result")
    public ResponseEntity<MatchResultResponse> recordResult(
            @PathVariable Long id,
            @Valid @RequestBody RecordMatchResultRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(matchResultService.recordResult(id, request, userDetails.getMemberId()));
    }

    @GetMapping("/{id}/result")
    public ResponseEntity<MatchResultResponse> getResult(@PathVariable Long id) {
        return ResponseEntity.ok(matchResultService.getResult(id));
    }
}
