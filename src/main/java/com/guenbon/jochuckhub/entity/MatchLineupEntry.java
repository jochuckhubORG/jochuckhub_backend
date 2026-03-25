package com.guenbon.jochuckhub.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "match_lineup_entry")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MatchLineupEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @Column(nullable = false)
    private int quarter; // 1~4

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Position position;

    @Builder
    public MatchLineupEntry(Match match, int quarter, Member member, Position position) {
        this.match = match;
        this.quarter = quarter;
        this.member = member;
        this.position = position;
    }
}
