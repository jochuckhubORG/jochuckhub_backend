package com.guenbon.jochuckhub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class CreateVirtualTeamRequest {

    @NotBlank(message = "가상 팀 이름은 필수입니다.")
    private String name;

    @NotNull(message = "가상 팀을 등록할 내 팀 ID는 필수입니다.")
    private Long myTeamId;
}
