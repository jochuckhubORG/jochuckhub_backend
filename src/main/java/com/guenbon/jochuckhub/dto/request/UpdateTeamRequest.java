package com.guenbon.jochuckhub.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class UpdateTeamRequest {

    @NotBlank(message = "팀 이름은 필수입니다.")
    private String name;
}
