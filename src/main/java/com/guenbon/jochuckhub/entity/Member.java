package com.guenbon.jochuckhub.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import java.util.HashSet;
import java.util.Set;

@Entity
@Audited
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "member")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;  // "kakao_{kakaoId}" 형식

    @Column(nullable = false, unique = true)
    private String kakaoId;

    @Column(nullable = false)
    private String name;  // 실제 이름 (예: 장지담)

    @Column
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Position mainPosition;

    @NotAudited
    @ElementCollection
    @CollectionTable(name = "member_sub_position", joinColumns = @JoinColumn(name = "member_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "position")
    private Set<Position> subPositions = new HashSet<>();

    @NotAudited
    @Column(length = 500)
    private String kakaoAccessToken;

    @Builder
    public Member(String kakaoId, String name,
                  Position mainPosition, Set<Position> subPositions) {
        this.kakaoId = kakaoId;
        this.username = "kakao_" + kakaoId;
        this.name = name;
        this.mainPosition = mainPosition;
        this.subPositions = subPositions != null ? subPositions : new HashSet<>();
    }

    public void update(String name, Position mainPosition, Set<Position> subPositions) {
        this.name = name;
        this.mainPosition = mainPosition;
        this.subPositions = subPositions != null ? subPositions : new HashSet<>();
    }

    public void updateKakaoAccessToken(String token) {
        this.kakaoAccessToken = token;
    }
}
