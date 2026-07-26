package com.guenbon.jochuckhub.dto.response;

import lombok.Getter;

@Getter
public class LoginResponse {

    private final String accessToken;
    private final String refreshToken;
    private final String tokenType = "Bearer";
    private final Long memberId;
    private final boolean isNewMember;

    public LoginResponse(String accessToken, String refreshToken, Long memberId, boolean isNewMember) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.memberId = memberId;
        this.isNewMember = isNewMember;
    }
}
