package com.guenbon.jochuckhub.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "team")
public class Team extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "name_key", nullable = false, unique = true)
    private String nameKey;

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

    @Column(nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TeamMember> teamMembers = new ArrayList<>();

    @Builder
    public Team(String name, boolean virtual, Long createdByTeamId) {
        this.name = name;
        this.virtual = virtual;
        this.createdByTeamId = createdByTeamId;
        this.nameKey = createNameKey(name, virtual, createdByTeamId);
    }

    public void updateName(String name) {
        this.name = name;
        this.nameKey = createNameKey(name, virtual, createdByTeamId);
    }

    public void deactivate() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
        this.nameKey = "DELETED:" + id + ":" + nameKey;
    }

    private static String createNameKey(String name, boolean virtual, Long createdByTeamId) {
        String normalizedName = name.trim().toLowerCase(Locale.ROOT);
        return virtual
                ? "VIRTUAL:" + createdByTeamId + ":" + normalizedName
                : "REAL:" + normalizedName;
    }
}
