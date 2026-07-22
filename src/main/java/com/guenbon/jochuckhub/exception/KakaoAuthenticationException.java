package com.guenbon.jochuckhub.exception;

import lombok.Getter;

@Getter
public class KakaoAuthenticationException extends RuntimeException {

    private final String reason;

    public KakaoAuthenticationException(String reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public KakaoAuthenticationException(String reason, String message) {
        super(message);
        this.reason = reason;
    }
}
