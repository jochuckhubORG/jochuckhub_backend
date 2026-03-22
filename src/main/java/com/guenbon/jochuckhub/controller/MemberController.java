package com.guenbon.jochuckhub.controller;

import com.guenbon.jochuckhub.dto.request.SignUpRequest;
import com.guenbon.jochuckhub.dto.response.MemberResponse;
import com.guenbon.jochuckhub.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @PostMapping("/signup")
    public ResponseEntity<Void> signUp(@Valid @RequestBody SignUpRequest signUpRequest) {
        memberService.register(signUpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
