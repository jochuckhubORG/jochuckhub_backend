package com.guenbon.jochuckhub.dto.response;

import com.guenbon.jochuckhub.entity.ActualAttendStatus;
import com.guenbon.jochuckhub.entity.AttendStatus;
import com.guenbon.jochuckhub.entity.MatchVote;
import lombok.Getter;

@Getter
public class MatchVoteResponse {

    private final Long voteId;
    private final Long matchId;
    private final Long memberId;
    private final String memberName;
    private final AttendStatus attendStatus;
    // null이면 정상 참석 (매치 시작 후 ATTEND 투표자에게만 적용)
    private final ActualAttendStatus actualStatus;

    public MatchVoteResponse(MatchVote vote) {
        this.voteId = vote.getId();
        this.matchId = vote.getMatch().getId();
        this.memberId = vote.getMember().getId();
        this.memberName = vote.getMember().getName();
        this.attendStatus = vote.getAttendStatus();
        this.actualStatus = vote.getActualStatus();
    }
}
