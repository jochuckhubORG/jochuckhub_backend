package com.guenbon.jochuckhub.dto.response;

import com.guenbon.jochuckhub.service.RefreshTokenService;

public record TokenResponse(String accessToken, String refreshToken, String tokenType) {

    public static TokenResponse from(RefreshTokenService.TokenPair tokenPair) {
        return new TokenResponse(tokenPair.accessToken(), tokenPair.refreshToken(), "Bearer");
    }
}
