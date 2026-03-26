package com.guenbon.jochuckhub.dto.response;

import lombok.Getter;

@Getter
public class LoginResponse {

    private final String accessToken;
    private final String tokenType = "Bearer";
    private final Long memberId;

    public LoginResponse(String accessToken, Long memberId) {
        this.accessToken = accessToken;
        this.memberId = memberId;
    }
}
