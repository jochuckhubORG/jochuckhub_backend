package com.guenbon.jochuckhub.config.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Writes security events in a stable, searchable key=value form.
 * Never include credentials, JWTs, cookies, authorization headers, or personal data in these logs.
 */
@Component
@Slf4j
public class SecurityEventLogger {

    public void jwtAuthenticationSucceeded(String username, String method, String path) {
        log.debug("security_event=jwt_authentication status=success subject={} method={} path={}",
                username, method, path);
    }

    public void jwtAuthenticationFailed(String reason, String method, String path) {
        log.warn("security_event=jwt_authentication status=failure reason={} method={} path={}",
                reason, method, path);
    }

    public void csrfRejected(String method, String path) {
        log.warn("security_event=csrf_validation status=failure reason=invalid_or_missing_token method={} path={}",
                method, path);
    }
}
