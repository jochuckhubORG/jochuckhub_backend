package com.guenbon.jochuckhub.config.jwt;

import com.guenbon.jochuckhub.config.security.SecurityEventLogger;
import com.guenbon.jochuckhub.dto.CustomUserDetails;
import com.guenbon.jochuckhub.service.CustomUserDetailsService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final SecurityEventLogger securityEventLogger;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = resolveToken(request);

        if (StringUtils.hasText(token)) {
            try {
                String username = jwtTokenProvider.validateAndGetUsername(token);
                CustomUserDetails userDetails =
                        (CustomUserDetails) customUserDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
                securityEventLogger.jwtAuthenticationSucceeded(
                        username, request.getMethod(), request.getRequestURI());
            } catch (ExpiredJwtException e) {
                SecurityContextHolder.clearContext();
                logFailure("expired", request);
            } catch (SignatureException e) {
                SecurityContextHolder.clearContext();
                logFailure("invalid_signature", request);
            } catch (MalformedJwtException e) {
                SecurityContextHolder.clearContext();
                logFailure("malformed", request);
            } catch (UnsupportedJwtException e) {
                SecurityContextHolder.clearContext();
                logFailure("unsupported", request);
            } catch (IllegalArgumentException e) {
                SecurityContextHolder.clearContext();
                logFailure("invalid", request);
            } catch (Exception e) {
                SecurityContextHolder.clearContext();
                logFailure("authentication_error", request);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        // HttpOnly 쿠키에서 accessToken 추출 (카카오 로그인 흐름)
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private void logFailure(String reason, HttpServletRequest request) {
        securityEventLogger.jwtAuthenticationFailed(reason, request.getMethod(), request.getRequestURI());
    }
}
