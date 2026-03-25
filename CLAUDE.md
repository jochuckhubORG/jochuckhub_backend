# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 개요

**조축허브(JochukhHub)** — 조기 축구 팀 관리 웹 애플리케이션 백엔드.
Java 17 + Spring Boot 3.3.4 + Spring Security (JWT Stateless) + Spring Data JPA + MySQL

## 개발 명령어

```bash
# Windows
gradlew.bat build
gradlew.bat bootRun          # 포트 8080
gradlew.bat test
gradlew.bat test --tests "com.guenbon.jochuckhub.service.MemberServiceTest"

# Unix/WSL
./gradlew build
./gradlew bootRun
./gradlew test
```

## 아키텍처

### 인증 흐름

1. `POST /api/auth/login` → JWT 발급 (`{ accessToken, tokenType: "Bearer" }`)
2. 이후 모든 요청: `Authorization: Bearer <token>` 헤더
3. `JwtAuthenticationFilter` → `JwtTokenProvider` 검증 → `SecurityContext`에 `CustomUserDetails` 저장
4. 컨트롤러에서 `@AuthenticationPrincipal CustomUserDetails`로 현재 사용자 추출

공개 엔드포인트: `POST /api/auth/login`, `POST /api/members/signup`

### 권한 체계

팀 내 역할은 `TeamMember.role(TeamRole)`에서 관리 (Member 엔티티 자체에 role 없음).
- `OWNER` — 팀 삭제·수정 등 모든 권한
- `MANAGER` — 매치 생성, 가상 팀 등록 등 운영 권한
- `PLAYER` — 일반 팀원

권한 검증은 `TeamService.verifyOwner()` / `verifyOwnerOrManager()` 호출로 수행.

### 도메인 규칙

**Member**
- `mainPosition`(1개) + `subPositions`(`Set<Position>`, 최대 3개, 중복 불가)
- `@Audited` — Hibernate Envers로 변경 이력 추적

**Team**
- `virtual=false`: 실제 팀. 이름 uniqueness는 실제 팀 간에만 적용
- `virtual=true`: 가상 팀 (서비스 미가입 외부 팀). `createdByTeamId`에 만든 팀 ID 저장
- 가상 팀 이름 uniqueness: 동일 `createdByTeamId` 내에서만 적용
- 가상 팀은 만든 팀에게만 검색 노출

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
- 포메이션 슬롯: LB, CB×2, RB, CDM, CM×2, LW, ST, RW (총 10개)
- 참석 인원 유효 범위: 14 ≤ N ≤ 20 (범위 이탈 시 `IllegalArgumentException`)
- 수식: 3쿼터 플레이어 수 `x = 40 - 2N`, 2쿼터 플레이어 수 `y = 3N - 40`
- Phase 1: 출석율 점수 내림차순 → 남은 슬롯 많은 쿼터 우선 그리디 배정
- Phase 2: 쿼터별 헝가리안 알고리즘(O(n³))으로 포지션 만족도 최대화 (주포지션=2점, 부포지션=1점, 기타=0점)

### 참여 점수 (Attendance Score)
`GET /api/members/{id}/attendance-score?teamId=xxx` — 해당 팀에서의 최근 8경기 점수 합계

## API 엔드포인트

### 인증
| 메서드 | URL | 권한 |
|--------|-----|------|
| `POST` | `/api/auth/login` | 공개 |

### 회원
| 메서드 | URL | 권한 |
|--------|-----|------|
| `POST` | `/api/members/signup` | 공개 |
| `GET` | `/api/members` | 인증 |
| `GET` | `/api/members/{id}` | 인증 |
| `PUT` | `/api/members/{id}` | 인증(본인) |
| `GET` | `/api/members/{id}/attendance-score?teamId=xxx` | 인증 |

### 팀
| 메서드 | URL | 권한 |
|--------|-----|------|
| `POST` | `/api/teams` | 인증 |
| `GET` | `/api/teams` | 인증 |
| `GET` | `/api/teams/{id}` | 인증 |
| `PUT` | `/api/teams/{id}` | OWNER |
| `DELETE` | `/api/teams/{id}` | OWNER |
| `GET` | `/api/teams/search?name=xxx&myTeamId=yyy` | 인증 |
| `POST` | `/api/teams/virtual` | OWNER/MANAGER |

### 매치
| 메서드 | URL | 권한 |
|--------|-----|------|
| `POST` | `/api/matches` | OWNER/MANAGER |
| `GET` | `/api/matches?teamId=xxx` | 인증 |
| `GET` | `/api/matches/{id}` | 인증 |
| `PUT` | `/api/matches/{id}/result` | OWNER/MANAGER |
| `GET` | `/api/matches/{id}/result` | 인증 |

### 매치 투표
| 메서드 | URL | 권한 |
|--------|-----|------|
| `POST` | `/api/matches/{matchId}/votes` | 홈팀 멤버 |
| `PUT` | `/api/matches/{matchId}/votes` | 홈팀 멤버 |
| `GET` | `/api/matches/{matchId}/votes` | 홈팀 멤버 |
| `PATCH` | `/api/matches/{matchId}/votes/{memberId}/actual-status` | OWNER/MANAGER |

### 매치 라인업
| 메서드 | URL | 권한 |
|--------|-----|------|
| `POST` | `/api/matches/{matchId}/lineup` | OWNER/MANAGER |
| `GET` | `/api/matches/{matchId}/lineup` | 인증 |

## DB 설정

- MySQL, 포트 3306, DB명 `jochuckhub`, `ddl-auto=update`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

## 파일 관리

- 새로 추가된 파일이 있으면 작업 후 git add 한다.
- API 수정사항이 발생하면 README.md와 이 파일도 함께 수정한다.
