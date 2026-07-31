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

    private static final String BEARER_PREFIX = "Bearer ";

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
                authenticationFailed("expired", request);
            } catch (SignatureException e) {
                authenticationFailed("invalid_signature", request);
            } catch (MalformedJwtException e) {
                authenticationFailed("malformed", request);
            } catch (UnsupportedJwtException e) {
                authenticationFailed("unsupported", request);
            } catch (IllegalArgumentException e) {
                authenticationFailed("invalid", request);
            } catch (Exception e) {
                authenticationFailed("authentication_error", request);
            }
        }
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        return StringUtils.hasText(token) ? token : null;
    }

    private void authenticationFailed(String reason, HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        securityEventLogger.jwtAuthenticationFailed(reason, request.getMethod(), request.getRequestURI());
    }
}
