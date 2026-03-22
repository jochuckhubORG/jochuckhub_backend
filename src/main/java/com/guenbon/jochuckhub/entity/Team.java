package com.guenbon.jochuckhub.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "team")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /**
     * true: 다른 팀이 상대팀으로 등록한 가상 팀 (이 서비스에 가입하지 않은 팀)
     */
    @Column(name = "is_virtual", nullable = false)
    private boolean virtual = false;

    /**
     * 가상 팀을 등록한 팀의 ID. 가상 팀에만 값이 있음.
     */
    @Column(name = "created_by_team_id")
    private Long createdByTeamId;

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TeamMember> teamMembers = new ArrayList<>();

    @Builder
    public Team(String name, boolean virtual, Long createdByTeamId) {
        this.name = name;
        this.virtual = virtual;
        this.createdByTeamId = createdByTeamId;
    }

    public void updateName(String name) {
        this.name = name;
    }
}
