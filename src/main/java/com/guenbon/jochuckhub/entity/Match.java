package com.guenbon.jochuckhub.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "match_record")
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_team_id", nullable = false)
    private Team homeTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opponent_team_id", nullable = false)
    private Team opponentTeam;

    @Column(nullable = false)
    private LocalDateTime matchDate;

    @Column(nullable = false)
    private String location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private Member createdBy;

    @Builder
    public Match(Team homeTeam, Team opponentTeam, LocalDateTime matchDate, String location, Member createdBy) {
        this.homeTeam = homeTeam;
        this.opponentTeam = opponentTeam;
        this.matchDate = matchDate;
        this.location = location;
        this.createdBy = createdBy;
    }
}
