package com.guenbon.jochuckhub.controller;

import com.guenbon.jochuckhub.dto.CustomUserDetails;
import com.guenbon.jochuckhub.dto.request.SignUpRequest;
import com.guenbon.jochuckhub.dto.request.UpdateMemberRequest;
import com.guenbon.jochuckhub.dto.response.GoalRecordResponse;
import com.guenbon.jochuckhub.dto.response.MemberResponse;
import com.guenbon.jochuckhub.service.MemberService;
import com.guenbon.jochuckhub.service.StatsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final StatsService statsService;

    @GetMapping
    public ResponseEntity<List<MemberResponse>> getMembers() {
        return ResponseEntity.ok(memberService.getMembers());
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
            @RequestParam Long teamId) {
        return ResponseEntity.ok(memberService.getAttendanceScore(id, teamId));
    }

    /**
     * 선수 개인 기록 조회 (골/어시스트)
     *
     * @param id              조회할 선수 ID
     * @param teamId          소속 팀 ID (필수)
     * @param type            null=전체, GOAL=골만, ASSIST=어시스트만
     * @param sortDirection   DESC(기본, 최신순), ASC(오래된순)
     * @param opponentTeamId  특정 상대팀 ID
     * @param startDate       날짜 범위 시작 (포함, yyyy-MM-dd)
     * @param endDate         날짜 범위 종료 (포함, yyyy-MM-dd)
     * @param relatedMemberId 특정 팀원과 관련된 기록만 (골→어시스터, 어시스트→득점자)
     */
    @GetMapping("/{id}/goal-records")
    public ResponseEntity<List<GoalRecordResponse>> getGoalRecords(
            @PathVariable Long id,
            @RequestParam Long teamId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false, defaultValue = "DESC") String sortDirection,
            @RequestParam(required = false) Long opponentTeamId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long relatedMemberId) {
        return ResponseEntity.ok(statsService.getGoalRecords(
                id, teamId, type, sortDirection, opponentTeamId, startDate, endDate, relatedMemberId));
    }

    @PostMapping("/signup")
    public ResponseEntity<Void> signUp(@Valid @RequestBody SignUpRequest signUpRequest) {
        memberService.register(signUpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
