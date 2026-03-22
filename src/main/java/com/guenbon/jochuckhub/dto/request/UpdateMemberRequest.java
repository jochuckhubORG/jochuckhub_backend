package com.guenbon.jochuckhub.dto.request;

import com.guenbon.jochuckhub.entity.Position;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.HashSet;
import java.util.Set;

@Getter
public class UpdateMemberRequest {

    @NotBlank(message = "이름은 필수입니다.")
    private String name;

    @NotNull(message = "주 포지션은 필수입니다.")
    private Position mainPosition;

    @Size(max = 3, message = "서브 포지션은 최대 3개까지 등록할 수 있습니다.")
    private Set<Position> subPositions = new HashSet<>();
}
