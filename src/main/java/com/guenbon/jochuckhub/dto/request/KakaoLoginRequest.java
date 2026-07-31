package com.guenbon.jochuckhub.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class KakaoLoginRequest {

    @NotBlank(message = "카카오 access token은 필수입니다.")
    private String kakaoAccessToken;
}
