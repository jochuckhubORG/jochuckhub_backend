package com.guenbon.jochuckhub.controller;

import com.guenbon.jochuckhub.dto.response.LoginResponse;
import com.guenbon.jochuckhub.exception.KakaoAuthenticationException;
import com.guenbon.jochuckhub.service.KakaoAuthService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private static final String KAKAO_AUTHORIZE_URL = "https://kauth.kakao.com/oauth/authorize";

    private final KakaoAuthService kakaoAuthService;

    @Value("${kakao.client-id}")
    private String kakaoClientId;

    @Value("${kakao.redirect-uri}")
    private String kakaoRedirectUri;

    @Value("${kakao.frontend-redirect-uri}")
    private String frontendRedirectUri;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @GetMapping("/kakao")
    public ResponseEntity<Void> kakaoLoginRedirect() {
        URI kakaoAuthUri = UriComponentsBuilder.fromUriString(KAKAO_AUTHORIZE_URL)
                .queryParam("client_id", kakaoClientId)
                .queryParam("redirect_uri", kakaoRedirectUri)
                .queryParam("response_type", "code")
                .build()
                .encode()
                .toUri();
        return ResponseEntity.status(302).location(kakaoAuthUri).build();
    }

    @GetMapping("/kakao/callback")
    public void kakaoCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription,
            HttpServletResponse response) throws IOException {

        if (StringUtils.hasText(error)) {
            String frontendError = "access_denied".equals(error) ? "login_cancelled" : "login_failed";
            log.warn("auth_event=kakao_callback status=failure provider_error={} description_present={}",
                    error, StringUtils.hasText(errorDescription));
            redirectToFrontend(response, frontendError, null);
            return;
        }

        if (!StringUtils.hasText(code)) {
            log.warn("auth_event=kakao_callback status=failure reason=missing_code");
            redirectToFrontend(response, "login_failed", null);
            return;
        }

        try {
            LoginResponse loginResponse = kakaoAuthService.kakaoLogin(code);
            ResponseCookie jwtCookie = ResponseCookie.from("accessToken", loginResponse.getAccessToken())
                    .httpOnly(true)
                    // TODO(production): Use secure(true) once the service is served exclusively over HTTPS.
                    .secure(false)
                    .path("/")
                    .maxAge(jwtExpiration / 1000)
                    .sameSite("Lax")
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());
            redirectToFrontend(response, null, loginResponse.isNewMember());
        } catch (KakaoAuthenticationException e) {
            log.error("auth_event=kakao_callback status=failure reason={}", e.getReason());
            redirectToFrontend(response, "login_failed", null);
        }
    }

    private void redirectToFrontend(HttpServletResponse response, String error, Boolean newMember) throws IOException {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(frontendRedirectUri);
        if (error != null) {
            builder.queryParam("error", error);
        }
        if (Boolean.TRUE.equals(newMember)) {
            builder.queryParam("newMember", true);
        }
        response.sendRedirect(builder.build().encode().toUriString());
    }
}
