# 조축허브 ERD

> **렌더링 방법**
> - VS Code: `Markdown Preview Mermaid Support` 확장 설치 후 미리보기
> - 웹: [https://mermaid.live](https://mermaid.live) 에 아래 코드블록 내용 붙여넣기

---

```mermaid
erDiagram
    member {
        BIGINT id PK
        VARCHAR username UK "kakao_{kakaoId}"
        VARCHAR kakaoId UK
        VARCHAR name
        VARCHAR password
        VARCHAR mainPosition "enum: GK|CB|LB|RB|CDM|CM|LW|RW|ST"
    }

    member_sub_position {
        BIGINT member_id FK
        VARCHAR position "enum"
    }

    team {
        BIGINT id PK
        VARCHAR name
        BOOLEAN is_virtual
        BIGINT created_by_team_id "가상팀만 사용"
    }

    team_member {
        BIGINT id PK
        BIGINT team_id FK
        BIGINT member_id FK
        VARCHAR role "OWNER|MANAGER|PLAYER"
    }

    match_record {
        BIGINT id PK
        BIGINT home_team_id FK
        BIGINT opponent_team_id FK
        BIGINT created_by_id FK
        DATETIME match_date
        VARCHAR location
        INT duration_minutes
        DATETIME vote_deadline "null이면 matchDate-1h"
    }

    match_vote {
        BIGINT id PK
        BIGINT match_id FK
        BIGINT member_id FK
        VARCHAR attend_status "ATTEND|ABSENT"
        VARCHAR actual_status "null|LATE|NO_SHOW"
    }

    goal {
        BIGINT id PK
        BIGINT match_id FK
        BOOLEAN opponent_goal
        BIGINT scorer_id FK "홈팀 골일 때만"
        BIGINT assister_id FK "선택"
    }

    match_lineup_entry {
        BIGINT id PK
        BIGINT match_id FK
        BIGINT member_id FK
        INT quarter "1~4"
        VARCHAR position "enum"
    }

    member ||--o{ member_sub_position : "has"
    member ||--o{ team_member : "belongs to"
    team   ||--o{ team_member : "has"
    team   ||--o{ match_record : "home_team"
    team   ||--o{ match_record : "opponent_team"
    member ||--o{ match_record : "created_by"
    match_record ||--o{ match_vote : "has"
    member ||--o{ match_vote : "votes"
    match_record ||--o{ goal : "has"
    member ||--o{ goal : "scorer"
    member ||--o{ goal : "assister"
    match_record ||--o{ match_lineup_entry : "has"
    member ||--o{ match_lineup_entry : "assigned to"
```

---

## 테이블 요약

| 테이블 | 설명 |
|---|---|
| `member` | 회원 (카카오 연동) |
| `member_sub_position` | 부포지션 (최대 3개, 별도 컬렉션 테이블) |
| `team` | 실제 팀 + 가상 팀 |
| `team_member` | 팀-회원 N:M 연결, 역할 포함 |
| `match_record` | 경기 정보 (홈팀 vs 상대팀) |
| `match_vote` | 경기별 출석 투표, unique(match_id, member_id) |
| `goal` | 골 기록, 홈/상대팀 구분 |
| `match_lineup_entry` | 쿼터별 라인업 배정 |
