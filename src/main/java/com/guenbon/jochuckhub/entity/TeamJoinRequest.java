package com.guenbon.jochuckhub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "team_join_request", uniqueConstraints =
        @UniqueConstraint(columnNames = {"team_id", "member_id"}))
public class TeamJoinRequest extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JoinRequestStatus status;

    @Builder
    public TeamJoinRequest(Team team, Member member, JoinRequestStatus status) {
        this.team = team;
        this.member = member;
        this.status = status;
    }

    public void resubmit() {
        this.status = JoinRequestStatus.PENDING;
    }

    public void approve() {
        ensurePending();
        this.status = JoinRequestStatus.APPROVED;
    }

    public void reject() {
        ensurePending();
        this.status = JoinRequestStatus.REJECTED;
    }

    private void ensurePending() {
        if (status != JoinRequestStatus.PENDING) {
            throw new IllegalArgumentException("Only pending join requests can be processed.");
        }
    }
}
