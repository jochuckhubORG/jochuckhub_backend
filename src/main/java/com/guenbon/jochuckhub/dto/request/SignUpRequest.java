package com.guenbon.jochuckhub.dto.request;

import com.guenbon.jochuckhub.entity.Position;
import com.guenbon.jochuckhub.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
public class SignUpRequest {

    @NotBlank(message = "아이디를 입력해주세요.")
    private String username;

    @NotBlank(message = "비밀번호를 입력해주세요.")
    private String password;

    @NotBlank(message = "이름을 입력해주세요.")
    private String name;

    @NotNull(message = "역할을 선택해주세요.")
    private Role role;

    @NotNull(message = "주 포지션을 선택해주세요.")
    private Position mainPosition;

    @Size(max = 3, message = "서브 포지션은 최대 3개까지 선택 가능합니다.")
    private Set<Position> subPositions = new LinkedHashSet<>();
}
