package com.guenbon.jochuckhub.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "goal")
public class Goal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    // true면 상대팀 골 (scorer, assister는 null)
    @Column(nullable = false)
    private boolean opponentGoal;

    // 홈팀 골일 때만 사용
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scorer_id")
    private Member scorer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assister_id")
    private Member assister;

    @Builder
    public Goal(Match match, boolean opponentGoal, Member scorer, Member assister) {
        this.match = match;
        this.opponentGoal = opponentGoal;
        this.scorer = scorer;
        this.assister = assister;
    }
}
