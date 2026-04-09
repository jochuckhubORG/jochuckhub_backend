# 조축허브 API 레퍼런스

**Base URL**: `http://localhost:8080`
**Content-Type**: `application/json`
**Swagger UI**: `http://localhost:8080/swagger-ui/index.html`

---

## 최근 작업 (CLAUDE CODE)

- 2026-04-10: 카카오 OAuth2 로그인으로 변경 — `POST /api/auth/login` 제거, `POST /api/auth/kakao` + `GET /api/auth/kakao` 추가, `POST /api/members/signup` 제거
- 2026-04-05: `GET /api/teams/{id}/members` 팀원 목록+통계 추가, `GET /api/members/{id}/goal-records` 선수 기록 조회(다중 필터) 추가
- 2026-04-05: `POST /api/teams/{id}/join` 팀 가입 API 추가

---

## 엔드포인트 요약

| 메서드 | URL | 권한 | 설명 |
|--------|-----|------|------|
| `GET` | `/api/auth/kakao` | 공개 | 카카오 로그인 페이지로 리다이렉트 |
| `POST` | `/api/auth/kakao` | 공개 | 카카오 인가코드 → JWT 발급 (로그인/자동가입) |
| `GET` | `/api/members` | 인증 | 전체 회원 목록 |
| `GET` | `/api/members/{id}` | 인증 | 회원 상세 조회 |
| `PUT` | `/api/members/{id}` | 인증(본인) | 회원 프로필 수정 |
| `GET` | `/api/members/{id}/attendance-score` | 인증 | 팀 내 참여 점수 조회 |
| `GET` | `/api/members/{id}/goal-records` | 인증 | 선수 기록 조회 (골/어시스트, 다중 필터) |
| `POST` | `/api/teams` | 인증 | 팀 생성 |
| `GET` | `/api/teams` | 인증 | 내 소속 팀 목록 |
| `GET` | `/api/teams/search` | 인증 | 팀 이름 검색 |
| `POST` | `/api/teams/virtual` | OWNER/MANAGER | 가상 팀 생성 |
| `GET` | `/api/teams/{id}` | 인증 | 팀 상세 조회 |
| `GET` | `/api/teams/{id}/members` | 인증 | 팀원 목록 + 통계 (골/어시스트/출전경기) |
| `POST` | `/api/teams/{id}/join` | 인증 | 팀 가입 (PLAYER) |
| `PUT` | `/api/teams/{id}` | OWNER | 팀 정보 수정 |
| `DELETE` | `/api/teams/{id}` | OWNER | 팀 삭제 |
| `POST` | `/api/matches` | OWNER/MANAGER | 매치 생성 |
| `GET` | `/api/matches` | 인증 | 팀별 매치 목록 |
| `GET` | `/api/matches/{id}` | 인증 | 매치 상세 조회 |
| `PUT` | `/api/matches/{id}/result` | OWNER/MANAGER | 매치 결과 기록 |
| `GET` | `/api/matches/{id}/result` | 인증 | 매치 결과 조회 |
| `POST` | `/api/matches/{matchId}/votes` | 홈팀 멤버 | 투표 등록 |
| `PUT` | `/api/matches/{matchId}/votes` | 홈팀 멤버 | 투표 수정 |
| `GET` | `/api/matches/{matchId}/votes` | 홈팀 멤버 | 투표 결과 조회 |
| `PATCH` | `/api/matches/{matchId}/votes/{memberId}/actual-status` | OWNER/MANAGER | 실제 출석 상태 설정 |
| `POST` | `/api/matches/{matchId}/lineup` | OWNER/MANAGER | 라인업 자동 생성 |
| `PUT` | `/api/matches/{matchId}/lineup` | OWNER/MANAGER | 라인업 직접 저장 |
| `GET` | `/api/matches/{matchId}/lineup` | 인증 | 라인업 조회 |

---

## 인증

JWT Bearer 토큰 방식을 사용합니다.

```
Authorization: Bearer <accessToken>
```

공개 엔드포인트 (인증 불필요): `POST /api/auth/login`, `POST /api/members/signup`

---

## 공통 오류 응답

```json
{
  "code": "ERROR_CODE",
  "message": "오류 설명"
}
```

| HTTP 상태 | code | 설명 |
|-----------|------|------|
| 400 | `VALIDATION_ERROR` | 요청 값 검증 실패 |
| 400 | `BAD_REQUEST` | 비즈니스 규칙 위반 |
| 403 | `FORBIDDEN` | 권한 부족 |
| 404 | `MEMBER_NOT_FOUND` | 회원 없음 |
| 404 | `TEAM_NOT_FOUND` | 팀 없음 |
| 500 | `INTERNAL_SERVER_ERROR` | 서버 오류 |

---

## 공통 타입

**Position** (포지션 enum)
```
GK | CB | LB | RB | CDM | CM | LW | RW | ST
```

**TeamRole** (팀 내 역할 enum)
```
OWNER | MANAGER | PLAYER
```

**AttendStatus** (투표 상태 enum)
```
ATTEND | ABSENT
```

**ActualAttendStatus** (실제 출석 상태 enum, null = 정상 참석)
```
LATE | NO_SHOW
```

---

## 1. 인증 API

### POST /api/auth/login
로그인 후 JWT 토큰을 발급합니다.

**인증 불필요**

**Request Body**
```json
{
  "username": "string (필수)",
  "password": "string (필수)"
}
```

**Response 200**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "memberId": 1
}
```

---

## 2. 회원 API

### POST /api/members/signup
신규 회원가입을 합니다.

**인증 불필요**

**Request Body**
```json
{
  "username": "string (필수)",
  "password": "string (필수)",
  "name": "string (필수)",
  "mainPosition": "ST (필수)",
  "subPositions": ["LW", "RW"]
}
```
> `subPositions`: 최대 3개, 중복 불가. 생략 시 빈 배열.

**Response 201** (본문 없음)

---

### GET /api/members
전체 회원 목록을 조회합니다.

**인증 필요**

**Response 200**
```json
[
  {
    "id": 1,
    "username": "user1",
    "name": "홍길동",
    "mainPosition": "ST",
    "subPositions": ["LW", "RW"]
  }
]
```

---

### GET /api/members/{id}
특정 회원 정보를 조회합니다.

**인증 필요**

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| id | Long | 회원 ID |

**Response 200**
```json
{
  "id": 1,
  "username": "user1",
  "name": "홍길동",
  "mainPosition": "ST",
  "subPositions": ["LW", "RW"]
}
```

---

### PUT /api/members/{id}
회원 프로필을 수정합니다. 본인만 가능합니다.

**인증 필요 (본인)**

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| id | Long | 회원 ID |

**Request Body**
```json
{
  "name": "string (필수)",
  "mainPosition": "ST (필수)",
  "subPositions": ["LW", "RW"]
}
```
> `subPositions`: 최대 3개. 생략 시 빈 배열.

**Response 200** — `MemberResponse` (위와 동일)

---

### GET /api/members/{id}/attendance-score?teamId={teamId}
특정 팀에서의 회원 참여 점수를 조회합니다. 최근 8경기 기준.

**인증 필요**

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| id | Long | 회원 ID |

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| teamId | Long | 필수 | 팀 ID |

**점수 기준**

| 상태 | 점수 |
|------|------|
| ATTEND (정상) | 2점 |
| LATE (지각) | 1점 |
| ABSENT (불참) | 0점 |
| NO_SHOW (무단불참) | -1점 |

**Response 200**
```json
14
```

---

## 3. 팀 API

### POST /api/teams
새 팀을 생성합니다. 생성자는 자동으로 OWNER가 됩니다.

**인증 필요**

**Request Body**
```json
{
  "name": "string (필수)"
}
```
> 팀 이름은 실제 팀 간 중복 불가.

**Response 201** — `TeamDetailResponse`
```json
{
  "id": 1,
  "name": "FC 서울",
  "virtual": false,
  "owner": {
    "id": 1,
    "username": "user1",
    "name": "홍길동",
    "mainPosition": "ST",
    "subPositions": []
  },
  "managers": [],
  "memberCount": 1,
  "currentUserRole": "OWNER"
}
```

---

### GET /api/teams
내가 소속된 팀 목록을 조회합니다.

**인증 필요**

**Response 200** — `TeamSummaryResponse[]`
```json
[
  {
    "id": 1,
    "name": "FC 서울",
    "virtual": false,
    "memberCount": 11
  }
]
```

---

### GET /api/teams/search?name={name}&myTeamId={myTeamId}
팀 이름으로 검색합니다.

**인증 필요**

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| name | String | 필수 | 검색할 팀 이름 |
| myTeamId | Long | 선택 | 내 팀 ID. 지정 시 해당 팀이 만든 가상 팀도 포함 |

**Response 200** — `TeamSummaryResponse[]`

---

### POST /api/teams/virtual
가상 팀을 생성합니다. (서비스 미가입 외부 팀 등록용)

**인증 필요 (OWNER 또는 MANAGER)**

**Request Body**
```json
{
  "name": "string (필수)",
  "myTeamId": 1
}
```
> 가상 팀 이름은 동일 `myTeamId` 내에서 중복 불가.

**Response 201** — `TeamSummaryResponse`
```json
{
  "id": 10,
  "name": "외부팀A",
  "virtual": true,
  "memberCount": 0
}
```

---

### GET /api/teams/{id}
팀 상세 정보를 조회합니다.

**인증 필요**

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| id | Long | 팀 ID |

**Response 200** — `TeamDetailResponse`
> `currentUserRole`: 팀 미소속이면 `null`

---

### POST /api/teams/{id}/join
팀에 가입합니다. 가입 후 역할은 `PLAYER`.

**인증 필요**

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| id | Long | 팀 ID |

**Response 200** (본문 없음)

---

### PUT /api/teams/{id}
팀 정보를 수정합니다.

**인증 필요 (OWNER)**

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| id | Long | 팀 ID |

**Request Body**
```json
{
  "name": "string (필수)"
}
```

**Response 200** — `TeamDetailResponse`

---

### DELETE /api/teams/{id}
팀을 삭제합니다.

**인증 필요 (OWNER)**

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| id | Long | 팀 ID |

**Response 204** (본문 없음)

---

## 4. 매치 API

### POST /api/matches
새 매치를 생성합니다.

**인증 필요 (홈팀의 OWNER 또는 MANAGER)**

**제약 조건**
- `matchDate`는 현재 시각 기준 최소 2시간 이후
- `opponentTeamId`가 가상 팀이면 반드시 홈팀이 만든 가상 팀이어야 함

**Request Body**
```json
{
  "homeTeamId": 1,
  "opponentTeamId": 2,
  "matchDate": "2026-05-01T14:00:00",
  "location": "월드컵 경기장",
  "durationMinutes": 80,
  "voteDeadline": "2026-05-01T13:00:00"
}
```
> `voteDeadline`: 생략 시 `matchDate - 1시간` 자동 적용.

**Response 201** — `MatchResponse`
```json
{
  "id": 1,
  "homeTeam": { "id": 1, "name": "FC 서울", "virtual": false },
  "opponentTeam": { "id": 2, "name": "외부팀A", "virtual": true },
  "matchDate": "2026-05-01T14:00:00",
  "durationMinutes": 80,
  "matchEndTime": "2026-05-01T15:20:00",
  "location": "월드컵 경기장",
  "createdBy": "홍길동",
  "voteDeadline": "2026-05-01T13:00:00"
}
```

---

### GET /api/matches?teamId={teamId}
팀의 매치 목록을 조회합니다.

**인증 필요**

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| teamId | Long | 필수 | 팀 ID |

**Response 200** — `MatchResponse[]`

---

### GET /api/matches/{id}
매치 상세 정보를 조회합니다.

**인증 필요**

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| id | Long | 매치 ID |

**Response 200** — `MatchResponse`

---

### PUT /api/matches/{id}/result
매치 결과를 기록합니다. 경기 종료 후에만 가능. 재호출 시 덮어씁니다.

**인증 필요 (홈팀의 OWNER 또는 MANAGER)**

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| id | Long | 매치 ID |

**Request Body**
```json
{
  "goals": [
    {
      "opponentGoal": false,
      "scorerMemberId": 3,
      "assisterMemberId": 5
    },
    {
      "opponentGoal": true
    }
  ],
  "lateMemberIds": [7],
  "noShowMemberIds": [8]
}
```
> `goals`: 빈 배열 가능, `null` 불가.
> `opponentGoal=true`이면 상대팀 골 (scorerMemberId, assisterMemberId 무시).
> `opponentGoal=false`이면 홈팀 골 (scorerMemberId 필수, assisterMemberId 선택).
> `lateMemberIds`, `noShowMemberIds`: ATTEND 투표자 중 해당 멤버에게만 적용.

**Response 200** — `MatchResultResponse`
```json
{
  "matchId": 1,
  "homeScore": 2,
  "opponentScore": 1,
  "goals": [
    {
      "id": 1,
      "opponentGoal": false,
      "scorerId": 3,
      "scorerName": "김철수",
      "assisterId": 5,
      "assisterName": "이영희"
    },
    {
      "id": 2,
      "opponentGoal": true,
      "scorerId": null,
      "scorerName": null,
      "assisterId": null,
      "assisterName": null
    }
  ]
}
```

---

### GET /api/matches/{id}/result
매치 결과를 조회합니다.

**인증 필요**

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| id | Long | 매치 ID |

**Response 200** — `MatchResultResponse` (위와 동일)

---

## 5. 매치 투표 API

### POST /api/matches/{matchId}/votes
매치 참석 여부를 투표합니다.

**인증 필요 (홈팀 멤버, 투표 마감 전)**

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| matchId | Long | 매치 ID |

**Request Body**
```json
{
  "attendStatus": "ATTEND"
}
```
> `attendStatus`: `ATTEND` 또는 `ABSENT`

**Response 201** — `MatchVoteResponse`
```json
{
  "voteId": 1,
  "matchId": 1,
  "memberId": 3,
  "memberName": "김철수",
  "attendStatus": "ATTEND",
  "actualStatus": null
}
```

---

### PUT /api/matches/{matchId}/votes
기존 투표를 수정합니다.

**인증 필요 (홈팀 멤버, 투표 마감 전)**

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| matchId | Long | 매치 ID |

**Request Body** — `MatchVoteRequest` (위와 동일)

**Response 200** — `MatchVoteResponse`

---

### GET /api/matches/{matchId}/votes
매치 투표 결과를 조회합니다.

**인증 필요 (홈팀 멤버)**

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| matchId | Long | 매치 ID |

**Response 200** — `MatchVoteResultResponse`
```json
{
  "matchId": 1,
  "voteDeadline": "2026-05-01T13:00:00",
  "voteClosed": false,
  "matchStarted": false,
  "attendVotes": [
    {
      "voteId": 1,
      "matchId": 1,
      "memberId": 3,
      "memberName": "김철수",
      "attendStatus": "ATTEND",
      "actualStatus": null
    }
  ],
  "absentVotes": [],
  "notVotedMembers": [
    { "memberId": 7, "memberName": "박민수" }
  ],
  "attendCount": 1,
  "absentCount": 0,
  "notVotedCount": 1
}
```

---

### PATCH /api/matches/{matchId}/votes/{memberId}/actual-status
투표자의 실제 출석 상태를 설정합니다. 매치 시작 후, ATTEND 투표자에게만 적용 가능.

**인증 필요 (홈팀의 OWNER 또는 MANAGER)**

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| matchId | Long | 매치 ID |
| memberId | Long | 대상 회원 ID |

**Request Body**
```json
{
  "actualStatus": "LATE"
}
```
> `actualStatus`: `LATE` (지각), `NO_SHOW` (무단불참), `null` (정상 참석으로 초기화)

**Response 200** — `MatchVoteResponse`

---

## 6. 매치 라인업 API

### POST /api/matches/{matchId}/lineup
라인업을 자동 생성합니다. 기존 라인업이 있으면 덮어씁니다.

**인증 필요 (홈팀의 OWNER 또는 MANAGER)**

**전제 조건**
- 투표 마감 후
- ATTEND 투표자 14명 이상 20명 이하

**자동 생성 알고리즘**
1. 출석율 점수 내림차순 → 남은 슬롯 많은 쿼터 우선 그리디 배정
2. 쿼터별 헝가리안 알고리즘으로 포지션 만족도 최대화 (주포지션=2점, 부포지션=1점, 기타=0점)

**포메이션**: 4-3-3 고정 (LB, CB×2, RB, CDM, CM×2, LW, ST, RW — 쿼터당 10명, 4쿼터)

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| matchId | Long | 매치 ID |

**Response 200** — `MatchLineupResponse`
```json
{
  "matchId": 1,
  "quarters": [
    {
      "quarter": 1,
      "players": [
        {
          "memberId": 3,
          "memberName": "김철수",
          "assignedPosition": "ST",
          "positionFit": "MAIN"
        },
        {
          "memberId": 5,
          "memberName": "이영희",
          "assignedPosition": "LW",
          "positionFit": "SUB"
        }
      ]
    }
  ]
}
```
> `positionFit`: `MAIN` (주포지션), `SUB` (부포지션), `OTHER` (기타)

---

### PUT /api/matches/{matchId}/lineup
라인업을 직접 저장합니다. 기존 라인업이 있으면 덮어씁니다.

**인증 필요 (홈팀의 OWNER 또는 MANAGER)**

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| matchId | Long | 매치 ID |

**Request Body**
```json
{
  "quarters": [
    {
      "quarter": 1,
      "players": [
        { "memberId": 3, "position": "ST" },
        { "memberId": 5, "position": "LW" }
      ]
    }
  ]
}
```
> `quarters`: 정확히 4개 필수. 각 쿼터 `players`는 정확히 10명.

**Response 200** — `MatchLineupResponse`

---

### GET /api/matches/{matchId}/lineup
라인업을 조회합니다.

**인증 필요**

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| matchId | Long | 매치 ID |

**Response 200** — `MatchLineupResponse`
