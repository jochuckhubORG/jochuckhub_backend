# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 개요

**조축허브(JochukhHub)** — 조기 축구 팀 관리 웹 애플리케이션 백엔드.
Java 17 + Spring Boot 3.3.4 + Spring Security (JWT Stateless) + Spring Data JPA + MySQL

Git remote: `https://github.com/jd99iam/jochuckhub.git`

## 개발 명령어

```bash
# Windows
gradlew.bat build
gradlew.bat bootRun          # 포트 8080
gradlew.bat test
gradlew.bat test --tests "com.guenbon.jochuckhub.service.MemberServiceTest"
```

## 패키지 구조

```
src/main/java/com/guenbon/jochuckhub/
├── config/
│   └── jwt/           # JwtTokenProvider, JwtAuthenticationFilter, JwtAuthenticationEntryPoint
├── controller/        # AuthController, MemberController, TeamController, MatchController
│                      # MatchVoteController, MatchLineupController
├── service/           # MemberService, TeamService, MatchService, MatchVoteService
│                      # MatchResultService, MatchLineupService, CustomUserDetailsService
├── repository/        # MemberRepository, TeamRepository, TeamMemberRepository, MatchRepository
│                      # MatchVoteRepository, GoalRepository, MatchLineupEntryRepository
├── entity/            # Member, Team, TeamMember, Match, MatchVote, Goal, MatchLineupEntry
│                      # Position(enum), TeamRole(enum), AttendStatus(enum), ActualAttendStatus(enum)
├── dto/
│   ├── request/       # LoginRequest, SignUpRequest, CreateTeamRequest, CreateMatchRequest 등
│   └── response/      # LoginResponse, MemberResponse, TeamDetailResponse, MatchResponse 등
└── exception/         # GlobalExceptionHandler, MemberNotFoundException, TeamNotFoundException, ForbiddenException
```

## 인증 흐름

1. 프론트엔드가 `GET /api/auth/kakao?redirectUri=...` 또는 직접 카카오 URL로 사용자를 리다이렉트
2. 사용자가 카카오 로그인 → 카카오가 `redirectUri?code=xxx`로 리다이렉트
3. 프론트엔드가 `POST /api/auth/kakao { code, redirectUri }` 호출
4. 서버: 인가코드 → 카카오 액세스 토큰 → 카카오 사용자 정보 → Member 조회/생성 → JWT 발급
5. 응답: `{ accessToken, tokenType: "Bearer", memberId, isNewMember }`
   - `isNewMember=true` 이면 프론트에서 포지션 설정 (`PUT /api/members/{id}`) 유도
6. 이후 모든 요청: `Authorization: Bearer <token>` 헤더

공개 엔드포인트: `/api/auth/**`, `/swagger-ui/**`, `/v3/api-docs/**`

## 권한 체계

팀 내 역할은 `TeamMember.role(TeamRole)`에서 관리 (Member 엔티티 자체에 role 없음).
- `OWNER` — 팀 삭제·수정 등 모든 권한
- `MANAGER` — 매치 생성, 가상 팀 등록 등 운영 권한
- `PLAYER` — 일반 팀원

권한 검증: `TeamService.verifyOwner()` / `verifyOwnerOrManager()` 직접 호출.

## 도메인 규칙

**Member**
- `mainPosition`(1개) + `subPositions`(`Set<Position>`, 최대 3개, 중복 불가)
- `@Audited` — Hibernate Envers로 변경 이력 추적

**Team**
- `virtual=false`: 실제 팀. 이름 uniqueness는 실제 팀 간에만 적용
- `virtual=true`: 가상 팀 (서비스 미가입 외부 팀). `createdByTeamId`에 만든 팀 ID 저장
- 가상 팀 이름 uniqueness: 동일 `createdByTeamId` 내에서만 적용
- 가상 팀은 만든 팀에게만 검색 노출 (`searchByNameForTeam()` 쿼리)

**Match** (테이블명: `match_record`)
- `homeTeam`(실제 팀) + `opponentTeam`(실제 또는 가상 팀)
- `durationMinutes`: 경기 시간(분). `getMatchEndTime()` = matchDate + durationMinutes
- `voteDeadline`: null이면 자동으로 matchDate - 1시간 사용 (`getEffectiveVoteDeadline()`)
- 생성 조건: OWNER/MANAGER만, 현재 시각 기준 최소 2시간 이후
- 상대 팀이 가상 팀이면 반드시 내 팀이 만든 가상 팀이어야 함

**MatchVote**
- `(match_id, member_id)` unique constraint
- `attendStatus`: `ATTEND` / `ABSENT`
- `actualStatus`: null(정상) / `LATE`(지각) / `NO_SHOW`(무단불참) — 매치 시작 후 ATTEND 투표자에게만 OWNER/MANAGER가 설정
- 점수: ATTEND=2점, LATE=1점, ABSENT=0점, NO_SHOW=-1점

**Goal**
- `opponentGoal=true`이면 상대팀 골 (scorer, assister null)
- `opponentGoal=false`이면 홈팀 골 (scorer 필수, assister 선택)

**Position enum**: `GK`, `CB`, `LB`, `RB`, `CDM`, `CM`, `LW`, `RW`, `ST`

**MatchLineupEntry** (라인업 자동 생성, 4-3-3 고정 포메이션)
- 포메이션 슬롯: LB, CB×2, RB, CDM, CM×2, LW, ST, RW (총 10개/쿼터, 4쿼터)
- 참석 인원 유효 범위: 14 ≤ N ≤ 20 (범위 이탈 시 `IllegalArgumentException`)
- Phase 1: 출석율 점수 내림차순 → 남은 슬롯 많은 쿼터 우선 그리디 배정
- Phase 2: 쿼터별 헝가리안 알고리즘(O(n³))으로 포지션 만족도 최대화 (주포지션=2점, 부포지션=1점, 기타=0점)
- `positionFit` 응답값: `"MAIN"` / `"SUB"` / `"OTHER"`

**참여 점수 (Attendance Score)**
`GET /api/members/{id}/attendance-score?teamId=xxx` — 해당 팀에서의 최근 8경기 점수 합계

## API 엔드포인트

### 인증
| 메서드 | URL | 권한 |
|--------|-----|------|
| `GET` | `/api/auth/kakao?redirectUri=xxx` | 공개 (카카오 로그인 페이지로 리다이렉트) |
| `POST` | `/api/auth/kakao` | 공개 (인가코드 → JWT 발급) |

### 회원
| 메서드 | URL | 권한 |
|--------|-----|------|
| `GET` | `/api/members` | 인증 |
| `GET` | `/api/members/{id}` | 인증 |
| `PUT` | `/api/members/{id}` | 인증(본인) |
| `GET` | `/api/members/{id}/attendance-score?teamId=xxx` | 인증 |
| `GET` | `/api/members/{id}/goal-records?teamId=xxx[&type=GOAL\|ASSIST][&sortDirection=ASC\|DESC][&opponentTeamId=yyy][&startDate=yyyy-MM-dd][&endDate=yyyy-MM-dd][&relatedMemberId=zzz]` | 인증 |

### 팀
| 메서드 | URL | 권한 |
|--------|-----|------|
| `POST` | `/api/teams` | 인증 |
| `GET` | `/api/teams` | 인증 (내 소속 팀만 반환) |
| `GET` | `/api/teams/{id}` | 인증 (응답에 `currentUserRole` 포함) |
| `GET` | `/api/teams/{id}/members` | 인증 (팀원 목록 + 골/어시스트/출전경기 통계) |
| `PUT` | `/api/teams/{id}` | OWNER |
| `DELETE` | `/api/teams/{id}` | OWNER |
| `GET` | `/api/teams/search?name=xxx[&myTeamId=yyy]` | 인증 (myTeamId 없으면 실제 팀만 검색) |
| `POST` | `/api/teams/{id}/join` | 인증 (PLAYER로 가입) |
| `POST` | `/api/teams/virtual` | OWNER/MANAGER |

### 매치
| 메서드 | URL | 권한 |
|--------|-----|------|
| `POST` | `/api/matches` | OWNER/MANAGER |
| `GET` | `/api/matches?teamId=xxx` | 인증 |
| `GET` | `/api/matches/{id}` | 인증 |
| `PUT` | `/api/matches/{id}/result` | OWNER/MANAGER (경기 종료 후) |
| `GET` | `/api/matches/{id}/result` | 인증 |

### 매치 투표
| 메서드 | URL | 권한 |
|--------|-----|------|
| `POST` | `/api/matches/{matchId}/votes` | 홈팀 멤버 (투표 진행중) |
| `PUT` | `/api/matches/{matchId}/votes` | 홈팀 멤버 (투표 진행중) |
| `GET` | `/api/matches/{matchId}/votes` | 홈팀 멤버 |
| `PATCH` | `/api/matches/{matchId}/votes/{memberId}/actual-status` | OWNER/MANAGER (경기 시작 후) |

### 매치 라인업
| 메서드 | URL | 권한 |
|--------|-----|------|
| `POST` | `/api/matches/{matchId}/lineup` | OWNER/MANAGER (자동 생성, 투표 마감 후, 참석 14~20명) |
| `PUT` | `/api/matches/{matchId}/lineup` | OWNER/MANAGER (수동 저장) |
| `GET` | `/api/matches/{matchId}/lineup` | 인증 |

## 주요 DTO

**요청 DTO**
- `LoginRequest`: username, password
- `SignUpRequest`: username, password, name, mainPosition, subPositions
- `UpdateMemberRequest`: name, mainPosition, subPositions
- `CreateTeamRequest`: name
- `CreateVirtualTeamRequest`: name, myTeamId
- `CreateMatchRequest`: homeTeamId, opponentTeamId, matchDate, location, durationMinutes, voteDeadline
- `MatchVoteRequest`: attendStatus
- `UpdateActualStatusRequest`: actualStatus
- `RecordMatchResultRequest`: goals[], lateMemberIds[], noShowMemberIds[]
- `GoalRequest`: opponentGoal, scorerMemberId, assisterMemberId
- `SaveLineupRequest`: quarters[{quarter, players[{memberId, position}]}]

**응답 DTO**
- `LoginResponse`: accessToken, tokenType, memberId
- `MemberResponse`: id, username, name, mainPosition, subPositions
- `TeamSummaryResponse`: id, name, virtual, memberCount
- `TeamDetailResponse`: id, name, virtual, owner, managers[], memberCount, currentUserRole
- `TeamMemberStatsResponse`: id, username, name, mainPosition, subPositions, role, goals, assists, appearances
- `GoalRecordResponse`: matchId, matchDate, opponentTeamId, opponentTeamName, type(GOAL/ASSIST), relatedMemberId, relatedMemberName
- `MatchResponse`: id, homeTeam, opponentTeam, matchDate, durationMinutes, matchEndTime, location, createdBy, voteDeadline
- `MatchVoteResultResponse`: matchId, voteDeadline, voteClosed, matchStarted, attendVotes[], absentVotes[], notVotedMembers[], attendCount, absentCount, notVotedCount
- `MatchLineupResponse`: quarters[{quarter, players[{memberId, memberName, assignedPosition, positionFit}]}]
- `MatchResultResponse`: matchId, homeScore, opponentScore, goals[]

## 예외 처리

```
MethodArgumentNotValidException  → 400  VALIDATION_ERROR
MemberNotFoundException          → 404  MEMBER_NOT_FOUND
TeamNotFoundException            → 404  TEAM_NOT_FOUND
ForbiddenException               → 403  FORBIDDEN
IllegalArgumentException         → 400  BAD_REQUEST
Exception (기타)                  → 500  INTERNAL_SERVER_ERROR
```
응답 형식: `{ "code": "...", "message": "..." }`

## DB 설정

- MySQL, 포트 3306, DB명 `jochuckhub`, `ddl-auto=create-drop` (개발용)
- `spring.datasource.username=root`, `password=0000`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

## 파일 관리

- 새로 추가된 파일이 있으면 작업 후 git add 한다.
- API 수정사항이 발생하면 이 파일도 함께 수정한다.

## API_REFERENCE.md 관리 규칙

- **API를 추가하거나 수정할 때마다** `API_REFERENCE.md`도 반드시 함께 업데이트한다.
  - 엔드포인트 요약 테이블에 추가/수정
  - 해당 섹션의 상세 설명 추가/수정
- **작업 완료 후** `API_REFERENCE.md` 최상단의 `## 최근 작업 (CLAUDE CODE)` 섹션을 갱신한다.
  - 방금 수행한 작업 설명을 맨 위에 추가하고, 목록이 3개를 초과하면 가장 오래된 항목을 제거하여 최대 3개를 유지한다.
  - 형식: `- YYYY-MM-DD: <작업 설명> (예: POST /api/foo 추가, GET /api/bar 수정)`
