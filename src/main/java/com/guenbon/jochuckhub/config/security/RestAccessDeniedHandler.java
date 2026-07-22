package com.guenbon.jochuckhub.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guenbon.jochuckhub.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final SecurityEventLogger securityEventLogger;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {
        boolean csrfFailure = exception instanceof CsrfException;
        if (csrfFailure) {
            securityEventLogger.csrfRejected(request.getMethod(), request.getRequestURI());
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), new ErrorResponse(
                csrfFailure ? "CSRF_INVALID" : "FORBIDDEN",
                csrfFailure ? "CSRF 토큰이 없거나 유효하지 않습니다." : "접근 권한이 없습니다."
        ));
    }
}
