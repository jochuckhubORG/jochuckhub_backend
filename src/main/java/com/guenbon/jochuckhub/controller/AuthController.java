package com.guenbon.jochuckhub.controller;

import com.guenbon.jochuckhub.dto.request.KakaoLoginRequest;
import com.guenbon.jochuckhub.dto.request.RefreshTokenRequest;
import com.guenbon.jochuckhub.dto.response.LoginResponse;
import com.guenbon.jochuckhub.dto.response.TokenResponse;
import com.guenbon.jochuckhub.service.KakaoAuthService;
import com.guenbon.jochuckhub.service.RefreshTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final KakaoAuthService kakaoAuthService;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/kakao")
    public ResponseEntity<LoginResponse> kakaoLogin(@Valid @RequestBody KakaoLoginRequest request) {
        return ResponseEntity.ok(kakaoAuthService.kakaoLogin(request.getKakaoAccessToken()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        RefreshTokenService.TokenPair tokenPair = refreshTokenService.rotate(request.getRefreshToken());
        return ResponseEntity.ok(TokenResponse.from(tokenPair));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        refreshTokenService.revoke(request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }
}
