# 조축허브 (JochukhHub) - 백엔드

조기 축구 팀 관리 풀스택 웹 애플리케이션의 백엔드 서버입니다.

## 기술 스택

- **Java 17**
- **Spring Boot 3.3.4**
- **Spring Security** + JWT 인증 (JJWT 0.12.3)
- **Spring Data JPA** + Hibernate
- **Hibernate Envers** — 엔티티 변경 이력 감사(Audit)
- **MySQL**
- **Gradle**
- **Swagger (SpringDoc OpenAPI 2.5.0)**
- **Resilience4j** — 서킷브레이커
- **Spring Actuator** / **Spring AOP**

## 프로젝트 구조

```
src/main/java/com/guenbon/jochuckhub/
├── config/
│   ├── SecurityConfig.java              # Spring Security 설정 (JWT, Stateless)
│   └── jwt/
│       ├── JwtTokenProvider.java        # JWT 토큰 생성/검증
│       ├── JwtAuthenticationFilter.java # 요청마다 JWT 파싱 필터
│       └── JwtAuthenticationEntryPoint.java
├── controller/
│   ├── AuthController.java              # POST /api/auth/login
│   ├── MemberController.java            # /api/members
│   ├── TeamController.java              # /api/teams
│   └── MatchController.java             # /api/matches
├── dto/
│   ├── CustomUserDetails.java           # Spring Security UserDetails 구현체
│   ├── request/
│   │   ├── LoginRequest.java
│   │   ├── SignUpRequest.java
│   │   ├── UpdateMemberRequest.java
│   │   ├── CreateTeamRequest.java
│   │   ├── UpdateTeamRequest.java
│   │   ├── CreateVirtualTeamRequest.java
│   │   └── CreateMatchRequest.java
│   └── response/
│       ├── LoginResponse.java           # accessToken, tokenType
│       ├── MemberResponse.java
│       ├── TeamSummaryResponse.java     # virtual 필드 포함
│       ├── TeamDetailResponse.java      # owner, managers 포함
│       ├── MatchResponse.java
│       └── ErrorResponse.java
├── entity/
│   ├── Member.java                      # 회원 엔티티 (@Audited)
│   ├── Position.java                    # 축구 포지션 enum
│   ├── Role.java                        # 시스템 역할 enum
│   ├── Team.java                        # 팀 엔티티 (virtual 플래그 포함)
│   ├── TeamMember.java                  # 팀-회원 중간 테이블
│   ├── TeamRole.java                    # OWNER / MANAGER / PLAYER
│   └── Match.java                       # 매치 엔티티 (match_record 테이블)
├── exception/
│   ├── errorcode/ErrorCode.java
│   ├── GlobalExceptionHandler.java
│   ├── MemberNotFoundException.java
│   ├── TeamNotFoundException.java
│   └── ForbiddenException.java
├── repository/
│   ├── MemberRepository.java
│   ├── TeamRepository.java
│   ├── TeamMemberRepository.java
│   └── MatchRepository.java
└── service/
    ├── CustomUserDetailsService.java    # UserDetailsService 구현체
    ├── MemberService.java               # 회원 조회, 회원가입, 수정
    ├── TeamService.java                 # 팀 CRUD, 가상팀 생성, 팀 검색
    └── MatchService.java                # 매치 CRUD
```

## 도메인 모델

| 엔티티 | 설명 |
|--------|------|
| `Member` | 사용자. `mainPosition`(1개) + `subPositions`(최대 3개, 중복 불가). `@Audited`로 변경 이력 관리 |
| `Team` | 실제 팀 또는 가상 팀(`virtual=true`). 가상 팀은 서비스 미가입 외부 팀을 나타내며 생성한 팀에게만 노출 |
| `TeamMember` | 팀-회원 관계. `TeamRole` = `OWNER` / `MANAGER` / `PLAYER` |
| `Match` | 경기 기록(`match_record` 테이블). homeTeam(실제) + opponentTeam(실제 또는 가상) |

### Position enum

| 포지션군 | 값 |
|---------|---|
| 골키퍼 | `GK` |
| 수비수 | `CB`, `LB`, `RB`, `LWB`, `RWB` |
| 미드필더 | `CDM`, `CM`, `CAM`, `LM`, `RM` |
| 공격수 | `LW`, `RW`, `ST`, `CF` |

## API 엔드포인트

### 인증
| 메서드 | 경로 | 설명 | 인증 |
|--------|------|------|------|
| POST | `/api/auth/login` | 로그인 → JWT 토큰 발급 | 불필요 |

### 회원
| 메서드 | 경로 | 설명 | 인증 |
|--------|------|------|------|
| POST | `/api/members/signup` | 회원가입 | 불필요 |
| GET | `/api/members` | 전체 회원 목록 | 필요 |
| GET | `/api/members/{id}` | 회원 상세 | 필요 |
| PUT | `/api/members/{id}` | 회원 정보 수정 (본인만) | 필요 |

### 팀
| 메서드 | 경로 | 설명 | 인증 |
|--------|------|------|------|
| POST | `/api/teams` | 실제 팀 생성 (생성자 → OWNER) | 필요 |
| GET | `/api/teams` | 전체 팀 목록 | 필요 |
| GET | `/api/teams/{id}` | 팀 상세 | 필요 |
| PUT | `/api/teams/{id}` | 팀 이름 수정 | OWNER |
| DELETE | `/api/teams/{id}` | 팀 삭제 | OWNER |
| GET | `/api/teams/search?name=xxx&myTeamId=yyy` | 팀 이름 검색 | 필요 |
| POST | `/api/teams/virtual` | 가상 팀 생성 | OWNER/MANAGER |

### 매치
| 메서드 | 경로 | 설명 | 인증 |
|--------|------|------|------|
| POST | `/api/matches` | 매치 생성 | OWNER/MANAGER |
| GET | `/api/matches?teamId=xxx` | 팀의 매치 목록 | 필요 |
| GET | `/api/matches/{id}` | 매치 상세 | 필요 |

> Swagger UI: 서버 실행 후 `http://localhost:8080/swagger-ui/index.html`

## 인증 흐름

1. `POST /api/auth/login` → JWT 토큰 반환 (`{ accessToken, tokenType: "Bearer" }`)
2. 이후 모든 요청: `Authorization: Bearer <token>` 헤더 첨부
3. `JwtAuthenticationFilter`가 매 요청마다 토큰 검증 (만료: 24시간)
