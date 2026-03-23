package com.guenbon.jochuckhub.controller;

import com.guenbon.jochuckhub.dto.CustomUserDetails;
import com.guenbon.jochuckhub.dto.request.SignUpRequest;
import com.guenbon.jochuckhub.dto.request.UpdateMemberRequest;
import com.guenbon.jochuckhub.dto.response.MemberResponse;
import com.guenbon.jochuckhub.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

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

    @PostMapping("/signup")
    public ResponseEntity<Void> signUp(@Valid @RequestBody SignUpRequest signUpRequest) {
        memberService.register(signUpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
