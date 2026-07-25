package com.guenbon.jochuckhub.controller;

import com.guenbon.jochuckhub.dto.CustomUserDetails;
import com.guenbon.jochuckhub.dto.request.UpdateMemberRequest;
import com.guenbon.jochuckhub.dto.response.GoalRecordResponse;
import com.guenbon.jochuckhub.dto.response.MemberResponse;
import com.guenbon.jochuckhub.dto.response.PageResponse;
import com.guenbon.jochuckhub.service.MemberService;
import com.guenbon.jochuckhub.service.StatsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@Validated
public class MemberController {

    private final MemberService memberService;
    private final StatsService statsService;

    @GetMapping
    public ResponseEntity<PageResponse<MemberResponse>> getMembers(
            @RequestParam(defaultValue = "0") @Min(0) int page) {
        return ResponseEntity.ok(memberService.getMembers(page));
    }

    @GetMapping("/me")
    public ResponseEntity<MemberResponse> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(memberService.getMember(userDetails.getMemberId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MemberResponse> getMember(@PathVariable Long id) {
        return ResponseEntity.ok(memberService.getMember(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MemberResponse> updateMember(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMemberRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(memberService.updateMember(id, request, userDetails));
    }

    @GetMapping("/{id}/attendance-score")
    public ResponseEntity<Integer> getAttendanceScore(
            @PathVariable Long id,
            @RequestParam Long teamId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(memberService.getAttendanceScore(id, teamId, userDetails.getMemberId()));
    }

    @GetMapping("/{id}/goal-records")
    public ResponseEntity<PageResponse<GoalRecordResponse>> getGoalRecords(
            @PathVariable Long id,
            @RequestParam Long teamId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false, defaultValue = "DESC") String sortDirection,
            @RequestParam(required = false) Long opponentTeamId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long relatedMemberId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(statsService.getGoalRecords(
                id, teamId, type, sortDirection, opponentTeamId, startDate, endDate, relatedMemberId,
                userDetails.getMemberId(), page));
    }
}
