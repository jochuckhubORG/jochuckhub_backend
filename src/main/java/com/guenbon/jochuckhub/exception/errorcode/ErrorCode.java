package com.guenbon.jochuckhub.exception.errorcode;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    MEMBER_NOT_FOUND("member not found"),
    TEAM_NOT_FOUND("해당하는 팀을 찾을 수 없습니다."),

    // ===== JWT =====
    EXPIRED_TOKEN("Expired JWT token"),
    INVALID_SIGNATURE("Invalid JWT signature"),
    MALFORMED_TOKEN("Malformed JWT token"),
    UNSUPPORTED_TOKEN("Unsupported JWT token"),
    TOKEN_NOT_FOUND("Token is missing or empty"),
    TOKEN_MISMATCH("Token does not match server record");

    private String message;

    ErrorCode(String message) {
        this.message = message;
    }
}
