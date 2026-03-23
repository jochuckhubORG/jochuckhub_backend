package com.guenbon.jochuckhub.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

@Getter
public class RecordMatchResultRequest {

    @NotNull(message = "골 목록은 필수입니다. (없으면 빈 배열 전달)")
    private List<GoalRequest> goals;

    // ATTEND 투표자 중 지각 처리할 멤버 ID 목록
    private List<Long> lateMemberIds = Collections.emptyList();

    // ATTEND 투표자 중 무단불참 처리할 멤버 ID 목록
    private List<Long> noShowMemberIds = Collections.emptyList();
}
