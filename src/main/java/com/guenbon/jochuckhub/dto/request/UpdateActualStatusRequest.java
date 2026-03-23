package com.guenbon.jochuckhub.dto.request;

import com.guenbon.jochuckhub.entity.ActualAttendStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateActualStatusRequest {
    // null이면 정상 참석으로 초기화
    private ActualAttendStatus actualStatus;
}
