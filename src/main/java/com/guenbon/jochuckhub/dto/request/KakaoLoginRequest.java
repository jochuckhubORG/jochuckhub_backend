package com.guenbon.jochuckhub.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class KakaoLoginRequest {

    @NotBlank
    private String code;

    @NotBlank
    private String redirectUri;
}
