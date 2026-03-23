package com.guenbon.jochuckhub.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "match_vote",
        uniqueConstraints = @UniqueConstraint(columnNames = {"match_id", "member_id"}))
public class MatchVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendStatus attendStatus;

    // null이면 정상 참석 (매치 시작 후에만 의미 있음, ATTEND 투표자에게만 적용)
    @Enumerated(EnumType.STRING)
    @Column
    private ActualAttendStatus actualStatus;

    @Builder
    public MatchVote(Match match, Member member, AttendStatus attendStatus) {
        this.match = match;
        this.member = member;
        this.attendStatus = attendStatus;
    }

    public void updateStatus(AttendStatus attendStatus) {
        this.attendStatus = attendStatus;
    }

    public void updateActualStatus(ActualAttendStatus actualStatus) {
        this.actualStatus = actualStatus;
    }

    /**
     * 불참: 0점, 참석: 2점, 지각: 1점, 무단불참: -1점
     */
    public int getScore() {
        if (attendStatus == AttendStatus.ABSENT) return 0;
        if (actualStatus == null) return 2;
        return switch (actualStatus) {
            case LATE -> 1;
            case NO_SHOW -> -1;
        };
    }
}
