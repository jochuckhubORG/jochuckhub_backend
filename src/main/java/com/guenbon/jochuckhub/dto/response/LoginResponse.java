package com.guenbon.jochuckhub.dto.response;

import lombok.Getter;

@Getter
public class LoginResponse {

    private final String accessToken;
    private final String tokenType = "Bearer";
    private final Long memberId;
    private final boolean isNewMember;

    public LoginResponse(String accessToken, Long memberId, boolean isNewMember) {
        this.accessToken = accessToken;
        this.memberId = memberId;
        this.isNewMember = isNewMember;
    }
}
