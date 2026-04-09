package com.guenbon.jochuckhub.controller;

import com.guenbon.jochuckhub.dto.request.KakaoLoginRequest;
import com.guenbon.jochuckhub.dto.response.LoginResponse;
import com.guenbon.jochuckhub.service.KakaoAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final KakaoAuthService kakaoAuthService;

    @Value("${kakao.client-id}")
    private String kakaoClientId;

    /**
     * 카카오 로그인 페이지로 리다이렉트.
     * 프론트엔드가 직접 URL을 구성할 수도 있고, 이 엔드포인트로 리다이렉트를 요청할 수도 있습니다.
     * GET /api/auth/kakao?redirectUri=https://your-frontend.com/callback
     */
    @GetMapping("/kakao")
    public ResponseEntity<Void> kakaoLoginRedirect(@RequestParam String redirectUri) {
        String kakaoAuthUrl = "https://kauth.kakao.com/oauth/authorize"
                + "?client_id=" + kakaoClientId
                + "&redirect_uri=" + redirectUri
                + "&response_type=code";
        return ResponseEntity.status(302).location(URI.create(kakaoAuthUrl)).build();
    }

    /**
     * 카카오 인가 코드로 로그인/회원가입 처리 후 JWT 반환.
     * POST /api/auth/kakao
     * Body: { "code": "...", "redirectUri": "..." }
     */
    @PostMapping("/kakao")
    public ResponseEntity<LoginResponse> kakaoLogin(@Valid @RequestBody KakaoLoginRequest request) {
        LoginResponse response = kakaoAuthService.kakaoLogin(request.getCode(), request.getRedirectUri());
        return ResponseEntity.ok(response);
    }
}
