package com.guenbon.jochuckhub.controller;

import com.guenbon.jochuckhub.dto.response.LoginResponse;
import com.guenbon.jochuckhub.service.KakaoAuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final KakaoAuthService kakaoAuthService;

    @Value("${kakao.client-id}")
    private String kakaoClientId;

    @Value("${kakao.redirect-uri}")
    private String kakaoRedirectUri;

    @Value("${kakao.frontend-redirect-uri}")
    private String frontendRedirectUri;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    /**
     * 카카오 로그인 페이지로 리다이렉트.
     * GET /api/auth/kakao
     */
    @GetMapping("/kakao")
    public ResponseEntity<Void> kakaoLoginRedirect() {
        String kakaoAuthUrl = "https://kauth.kakao.com/oauth/authorize"
                + "?client_id=" + kakaoClientId
                + "&redirect_uri=" + kakaoRedirectUri
                + "&response_type=code";
        return ResponseEntity.status(302).location(URI.create(kakaoAuthUrl)).build();
    }

    /**
     * 카카오 인가코드 콜백. 카카오가 직접 리다이렉트하는 엔드포인트.
     * GET /api/auth/kakao/callback?code=xxx
     * 로그인 취소 시: ?error=access_denied&error_description=...
     */
    @GetMapping("/kakao/callback")
    public void kakaoCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            HttpServletResponse response) throws IOException {

        if (error != null) {
            response.sendRedirect(frontendRedirectUri + "?error=login_cancelled");
            return;
        }

        LoginResponse loginResponse = kakaoAuthService.kakaoLogin(code);

        ResponseCookie jwtCookie = ResponseCookie.from("accessToken", loginResponse.getAccessToken())
                .httpOnly(true)
                .secure(false)          // 운영 환경(HTTPS)에서는 true 로 변경
                .path("/")
                .maxAge(jwtExpiration / 1000)
                .sameSite("Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());

        String redirectUrl = frontendRedirectUri;
        if (loginResponse.isNewMember()) {
            redirectUrl += "?newMember=true";
        }
        response.sendRedirect(redirectUrl);
    }
}
