# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 개요

**조축허브(JochuckHub)** — 조기 축구 팀 관리 웹 애플리케이션 백엔드.
Java 17 + Spring Boot 3.3.4 + Spring Security (JWT Stateless) + Spring Data JPA + MySQL

Git remote: `https://github.com/jd99iam/jochuckhub.git`

## Git 제외 설정 파일

아래 파일들은 `.gitignore`에 등록되어 있어 저장소에 포함되지 않는다. 새 환경에서 직접 생성해야 한다.

### `src/main/resources/application-private.properties`

비밀 키 등 민감한 설정을 담는 파일. `application.properties`에서 `spring.config.import`로 자동 로드된다.

```properties
# DB 비밀번호
spring.datasource.password=<mysql_password>

# JWT 서명 키 (256비트 이상 Base64 인코딩)
jwt.secret=<jwt_secret>

# 카카오 OAuth2
kakao.client-id=<kakao_rest_api_key>
kakao.client-secret=<kakao_client_secret>
```

카카오 클라이언트 ID/Secret은 [카카오 개발자 콘솔](https://developers.kakao.com)에서 발급받는다.

`kakao.redirect-uri`(콜백 URL)는 콘솔의 "Redirect URI" 항목에 동일하게 등록해야 한다.

## 로컬 테스트 페이지

`src/main/resources/static/test-login.html` — 앱 실행 후 `http://localhost:8080/test-login.html`에서 접근 가능.

- 카카오 로그인 버튼 → `/api/auth/kakao` 리다이렉트 → 카카오 인증 → `/api/auth/kakao/callback` → 이 페이지로 복귀
- 로그인 성공 시 JWT가 `accessToken` HttpOnly 쿠키로 저장됨
- 회원 목록·팀 목록 조회, 팀 생성 등 기본 API를 브라우저에서 직접 테스트할 수 있음
- 같은 Origin(`localhost:8080`)이므로 CORS 설정 없이 동작

## 패키지 구조

```
src/main/java/com/guenbon/jochuckhub/
├── config/
│   ├── JpaConfig.java         # JPAQueryFactory 빈 등록
│   ├── SecurityConfig.java    # Spring Security, CSRF, CORS 설정
│   └── jwt/                   # JwtTokenProvider, JwtAuthenticationFilter, JwtAuthenticationEntryPoint
├── controller/        # AuthController, MemberController, TeamController, MatchController
│                      # MatchVoteController, MatchLineupController
├── service/           # MemberService, TeamService, MatchService, MatchVoteService
│                      # MatchResultService, MatchLineupService, KakaoAuthService, CustomUserDetailsService
├── repository/        # MemberRepository, TeamRepository, TeamMemberRepository, MatchRepository
│                      # MatchVoteRepository, GoalRepository, MatchLineupEntryRepository
├── entity/            # Member, Team, TeamMember, Match, MatchVote, Goal, MatchLineupEntry
│                      # Position(enum), TeamRole(enum), AttendStatus(enum), ActualAttendStatus(enum)
├── dto/
│   ├── request/       # UpdateMemberRequest, CreateTeamRequest, CreateMatchRequest 등
│   └── response/      # LoginResponse, MemberResponse, TeamDetailResponse, MatchResponse 등
└── exception/         # GlobalExceptionHandler, MemberNotFoundException, TeamNotFoundException, ForbiddenException
```

## 인증 흐름

1. 프론트엔드가 `GET /api/auth/kakao`로 사용자를 리다이렉트
2. 사용자가 카카오 로그인 → 카카오가 `GET /api/auth/kakao/callback?code=xxx`로 리다이렉트
3. 서버: 인가코드 → 카카오 액세스 토큰 → 카카오 사용자 정보 → Member 조회/생성 → JWT 발급
4. JWT를 `accessToken` **HttpOnly 쿠키**로 Set-Cookie → `kakao.frontend-redirect-uri`로 리다이렉트
   - 신규 가입이면 `?newMember=true` 파라미터 포함
5. 이후 모든 요청: 브라우저가 쿠키를 자동 포함 (`credentials: include`)
   - `JwtAuthenticationFilter`에서 쿠키의 `accessToken`을 추출해 인증 처리

공개 엔드포인트: `/api/auth/**`, `/swagger-ui/**`, `/v3/api-docs/**`, `/test-login.html`

## Spring Security 구성

**세션**: `STATELESS` — 서버 측 세션 없음, JWT 쿠키로만 인증 유지

**CSRF**: 비활성화 — JWT 쿠키 기반 Stateless REST API이므로 불필요

**CORS**: `application.properties`의 `cors.allowed-origins` 값으로 허용 Origin 지정
- `credentials: true` (쿠키 전송 허용)
- `Set-Cookie` 헤더 노출 (`exposedHeaders`)
- 허용 메서드: `GET, POST, PUT, PATCH, DELETE, OPTIONS`

**필터 체인 순서**
1. `JwtAuthenticationFilter` (커스텀) — `accessToken` 쿠키에서 JWT 추출 → 검증 → `SecurityContextHolder` 설정
2. `UsernamePasswordAuthenticationFilter` (Spring 기본, 실질적으로 비활성)

**인증 실패 처리**: `JwtAuthenticationEntryPoint` — 토큰 없거나 유효하지 않으면 401 반환

## 권한 체계

팀 내 역할은 `TeamMember.role(TeamRole)`에서 관리 (Member 엔티티 자체에 role 없음).
- `OWNER` — 팀 삭제·수정 등 모든 권한
- `MANAGER` — 매치 생성, 가상 팀 등록 등 운영 권한
- `PLAYER` — 일반 팀원

권한 검증: `TeamService.verifyOwner()` / `verifyOwnerOrManager()` 직접 호출.

## 도메인 규칙

**Member**
- `kakaoId` → DB 컬럼 `kakao_id`, `username`은 `"kakao_{kakaoId}"` 형식으로 자동 생성
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
| `GET` | `/api/auth/kakao` | 공개 (카카오 로그인 페이지로 리다이렉트) |
| `GET` | `/api/auth/kakao/callback?code=xxx` | 공개 (카카오 콜백 → JWT 쿠키 발급) |

### 회원
| 메서드 | URL | 권한 |
|--------|-----|------|
| `GET` | `/api/members` | 인증 |
| `GET` | `/api/members/me` | 인증 (현재 로그인한 사용자 정보) |
| `GET` | `/api/members/{id}` | 인증 |
| `PUT` | `/api/members/{id}` | 인증(본인) |
| `GET` | `/api/members/{id}/attendance-score?teamId=xxx` | 인증 |
| `GET` | `/api/members/{id}/goal-records?teamId=xxx[&type=GOAL\|ASSIST][&sortDirection=ASC\|DESC][&opponentTeamId=yyy][&startDate=yyyy-MM-dd][&endDate=yyyy-MM-dd][&relatedMemberId=zzz]` | 인증 |

> `relatedMemberId` 필터: `type=GOAL`이면 어시스트한 멤버, `type=ASSIST`이면 골을 넣은 멤버, `type` 없으면 해당 골에 관여한 멤버 전체

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
| `POST` | `/api/matches/{matchId}/lineup/announce` | OWNER/MANAGER (라인업 카카오톡 발표, talk_message 동의 필요) |

## 주요 DTO

**요청 DTO**
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
- `LoginResponse`: accessToken, tokenType, memberId, isNewMember
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
- username/password는 `application-private.properties`에서 설정
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

## 파일 관리

- 새로 추가된 파일이 있으면 작업 후 git add 한다.
- API 수정사항이 발생하면 이 파일(CLAUDE.md)도 함께 수정한다.
