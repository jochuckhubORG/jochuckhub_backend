package com.guenbon.jochuckhub.controller;

import com.guenbon.jochuckhub.dto.CustomUserDetails;
import com.guenbon.jochuckhub.dto.request.CreateMatchRequest;
import com.guenbon.jochuckhub.dto.response.MatchResponse;
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
}
