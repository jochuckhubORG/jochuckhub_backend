package com.guenbon.jochuckhub.dto.request;

import com.guenbon.jochuckhub.entity.AttendStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class MatchVoteRequest {

    @NotNull(message = "참석 여부는 필수입니다.")
    private AttendStatus attendStatus;
}
