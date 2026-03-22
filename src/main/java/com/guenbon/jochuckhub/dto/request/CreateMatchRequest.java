package com.guenbon.jochuckhub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CreateMatchRequest {

    @NotNull(message = "홈 팀 ID는 필수입니다.")
    private Long homeTeamId;

    @NotNull(message = "상대 팀 ID는 필수입니다.")
    private Long opponentTeamId;

    @NotNull(message = "경기 날짜/시간은 필수입니다.")
    private LocalDateTime matchDate;

    @NotBlank(message = "경기 장소는 필수입니다.")
    private String location;

    // 미입력 시 matchDate를 투표 마감 시점으로 사용
    private LocalDateTime voteDeadline;
}
