package com.guenbon.jochuckhub.dto.response;

import com.guenbon.jochuckhub.entity.JoinRequestStatus;
import com.guenbon.jochuckhub.entity.TeamJoinRequest;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class TeamJoinRequestResponse {

    private final Long id;
    private final Long memberId;
    private final String memberName;
    private final JoinRequestStatus status;
    private final LocalDateTime createdAt;

    public TeamJoinRequestResponse(TeamJoinRequest joinRequest) {
        this.id = joinRequest.getId();
        this.memberId = joinRequest.getMember().getId();
        this.memberName = joinRequest.getMember().getName();
        this.status = joinRequest.getStatus();
        this.createdAt = joinRequest.getCreatedAt();
    }
}
