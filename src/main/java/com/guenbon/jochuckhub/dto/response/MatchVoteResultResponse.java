package com.guenbon.jochuckhub.dto.response;

import com.guenbon.jochuckhub.entity.AttendStatus;
import com.guenbon.jochuckhub.entity.Member;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class MatchVoteResultResponse {

    private final Long matchId;
    private final LocalDateTime voteDeadline;
    private final boolean voteClosed;
    private final boolean matchStarted;
    // ATTEND 투표자 목록 (실제 출석 상태 포함)
    private final List<MatchVoteResponse> attendVotes;
    // ABSENT 투표자 목록
    private final List<MatchVoteResponse> absentVotes;
    // 미투표 멤버 목록
    private final List<NotVotedMemberResponse> notVotedMembers;
    private final int attendCount;
    private final int absentCount;
    private final int notVotedCount;

    public MatchVoteResultResponse(Long matchId, LocalDateTime voteDeadline, LocalDateTime matchDate,
                                   List<MatchVoteResponse> allVotes, List<Member> teamMembers) {
        this.matchId = matchId;
        this.voteDeadline = voteDeadline;
        this.voteClosed = LocalDateTime.now().isAfter(voteDeadline);
        this.matchStarted = LocalDateTime.now().isAfter(matchDate);

        this.attendVotes = allVotes.stream()
                .filter(v -> AttendStatus.ATTEND.equals(v.getAttendStatus()))
                .toList();
        this.absentVotes = allVotes.stream()
                .filter(v -> AttendStatus.ABSENT.equals(v.getAttendStatus()))
                .toList();

        java.util.Set<Long> votedMemberIds = allVotes.stream()
                .map(MatchVoteResponse::getMemberId)
                .collect(java.util.stream.Collectors.toSet());
        this.notVotedMembers = teamMembers.stream()
                .filter(m -> !votedMemberIds.contains(m.getId()))
                .map(NotVotedMemberResponse::new)
                .toList();

        this.attendCount = this.attendVotes.size();
        this.absentCount = this.absentVotes.size();
        this.notVotedCount = this.notVotedMembers.size();
    }

    @Getter
    public static class NotVotedMemberResponse {
        private final Long memberId;
        private final String memberName;

        public NotVotedMemberResponse(Member member) {
            this.memberId = member.getId();
            this.memberName = member.getName();
        }
    }
}
