package com.guenbon.jochuckhub.controller;

import com.guenbon.jochuckhub.dto.response.LoginResponse;
import com.guenbon.jochuckhub.exception.KakaoAuthenticationException;
import com.guenbon.jochuckhub.service.KakaoAuthService;
import com.guenbon.jochuckhub.service.RefreshTokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private static final String KAKAO_AUTHORIZE_URL = "https://kauth.kakao.com/oauth/authorize";
    private static final String OAUTH_STATE_COOKIE_NAME = "oauth_state";
    private static final String ACCESS_TOKEN_COOKIE_NAME = "accessToken";
    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final long OAUTH_STATE_MAX_AGE_SECONDS = 300;

    private final SecureRandom secureRandom = new SecureRandom();

    private final KakaoAuthService kakaoAuthService;
    private final RefreshTokenService refreshTokenService;

    @Value("${kakao.client-id}")
    private String kakaoClientId;

    @Value("${kakao.redirect-uri}")
    private String kakaoRedirectUri;

    @Value("${kakao.frontend-redirect-uri}")
    private String frontendRedirectUri;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @GetMapping("/kakao")
    public ResponseEntity<Void> kakaoLoginRedirect(HttpServletResponse response) {
        String state = createOAuthState();
        response.addHeader(HttpHeaders.SET_COOKIE, createOAuthStateCookie(state, OAUTH_STATE_MAX_AGE_SECONDS).toString());
        URI kakaoAuthUri = UriComponentsBuilder.fromUriString(KAKAO_AUTHORIZE_URL)
                .queryParam("client_id", kakaoClientId)
                .queryParam("redirect_uri", kakaoRedirectUri)
                .queryParam("response_type", "code")
                .queryParam("state", state)
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
            @RequestParam(required = false) String state,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        if (!isValidOAuthState(state, resolveCookieValue(request, OAUTH_STATE_COOKIE_NAME))) {
            log.warn("auth_event=kakao_callback status=failure reason=invalid_oauth_state");
            clearOAuthStateCookie(response);
            redirectToFrontend(response, "login_failed", null);
            return;
        }
        clearOAuthStateCookie(response);

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
            addAuthenticationCookies(response, loginResponse.getAccessToken(), loginResponse.getRefreshToken());
            redirectToFrontend(response, null, loginResponse.isNewMember());
        } catch (KakaoAuthenticationException e) {
            log.error("auth_event=kakao_callback status=failure reason={}", e.getReason());
            redirectToFrontend(response, "login_failed", null);
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = resolveCookieValue(request, REFRESH_TOKEN_COOKIE_NAME);
        RefreshTokenService.TokenPair tokenPair = refreshTokenService.rotate(refreshToken);
        addAuthenticationCookies(response, tokenPair.accessToken(), tokenPair.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        refreshTokenService.revoke(resolveCookieValue(request, REFRESH_TOKEN_COOKIE_NAME));
        clearAuthenticationCookies(response);
        return ResponseEntity.noContent().build();
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

    private String createOAuthState() {
        byte[] stateBytes = new byte[32];
        secureRandom.nextBytes(stateBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(stateBytes);
    }

    private ResponseCookie createOAuthStateCookie(String state, long maxAgeSeconds) {
        return ResponseCookie.from(OAUTH_STATE_COOKIE_NAME, state)
                .httpOnly(true)
                // TODO(production): Use secure(true) once the service is served exclusively over HTTPS.
                .secure(false)
                .path("/")
                .maxAge(maxAgeSeconds)
                .sameSite("Lax")
                .build();
    }

    private void addAuthenticationCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        response.addHeader(HttpHeaders.SET_COOKIE, createAuthenticationCookie(
                ACCESS_TOKEN_COOKIE_NAME, accessToken, "/", jwtExpiration / 1000).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, createAuthenticationCookie(
                REFRESH_TOKEN_COOKIE_NAME, refreshToken, "/api/auth", refreshExpiration / 1000).toString());
    }

    private void clearAuthenticationCookies(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, createAuthenticationCookie(ACCESS_TOKEN_COOKIE_NAME, "", "/", 0).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, createAuthenticationCookie(REFRESH_TOKEN_COOKIE_NAME, "", "/api/auth", 0).toString());
    }

    private ResponseCookie createAuthenticationCookie(String name, String value, String path, long maxAgeSeconds) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                // TODO(production): Use secure(true) once the service is served exclusively over HTTPS.
                .secure(false)
                .path(path)
                .maxAge(maxAgeSeconds)
                .sameSite("Lax")
                .build();
    }

    private boolean isValidOAuthState(String requestState, String cookieState) {
        return StringUtils.hasText(requestState)
                && StringUtils.hasText(cookieState)
                && MessageDigest.isEqual(
                requestState.getBytes(StandardCharsets.UTF_8),
                cookieState.getBytes(StandardCharsets.UTF_8));
    }

    private String resolveCookieValue(HttpServletRequest request, String cookieName) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void clearOAuthStateCookie(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, createOAuthStateCookie("", 0).toString());
    }
}
