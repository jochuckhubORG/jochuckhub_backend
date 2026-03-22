# 조축허브 (JochukhHub)

축구 팀 관리 웹 애플리케이션.

## 기술 스택

- **Java 17**, Spring Boot 3.3.4
- **Spring Security** — JWT 기반 Stateless 인증
- **Spring Data JPA** + MySQL
- **Hibernate Envers** — 엔티티 변경 이력 감사(Audit)
- **Lombok**

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
│       ├── LoginResponse.java           # JWT 토큰 반환
│       ├── MemberResponse.java
│       ├── TeamSummaryResponse.java     # virtual 필드 포함
│       ├── TeamDetailResponse.java      # virtual 필드 포함
│       ├── ErrorResponse.java
│       └── MatchResponse.java
├── entity/
│   ├── Member.java                      # 회원 엔티티 (@Audited)
│   ├── Position.java                    # 축구 포지션 enum
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

src/main/resources/
├── templates/                           # Thymeleaf (일부 잔존, 주 방식은 REST API)
│   ├── login.html
│   └── members/
│       ├── list.html
│       └── signup.html
├── static/css/
│   └── style.css
└── application.properties
```

## 도메인 규칙

### Member 엔티티
| 필드 | 설명 |
|------|------|
| `username` | 로그인 아이디 (unique) |
| `name` | 실제 이름 (예: 장지담) |
| `password` | BCrypt 암호화 |
| `mainPosition` | 주 포지션 (1개) |
| `subPositions` | 서브 포지션 (`Set<Position>`, 최대 3개) |

> Member 자체에는 role 필드 없음. 팀 내 역할은 TeamMember.role(TeamRole)로 관리.

### Position enum
- 골키퍼: `GK`
- 수비수: `CB`, `LB`, `RB`, `LWB`, `RWB`
- 미드필더: `CDM`, `CM`, `CAM`, `LM`, `RM`
- 공격수: `LW`, `RW`, `ST`, `CF`

### 포지션 제약
- `mainPosition`과 `subPositions` 간 중복 불가
- `subPositions` 간 중복 불가 (Set으로 자동 방지)
- `subPositions` 최대 3개

### Team 엔티티
| 필드 | 설명 |
|------|------|
| `name` | 팀 이름 (DB unique 없음, 서비스에서 검증) |
| `virtual` | 가상 팀 여부 (이 서비스에 가입하지 않은 외부 팀) |
| `createdByTeamId` | 가상 팀을 등록한 팀 ID (가상 팀에만 값 존재) |

#### 가상 팀(Virtual Team) 규칙
- 실제 팀 이름의 uniqueness: 실제 팀(`virtual=false`) 간에만 적용
- 가상 팀 이름의 uniqueness: 동일 createdByTeamId 내에서만 적용
- 가상 팀은 만든 팀에게만 검색에 노출 (타 팀에는 비공개)
- 가상 팀과 동일한 이름의 실제 팀이 이미 존재하면 생성 불가

### TeamRole enum
- `OWNER` — 팀 삭제, 이름 수정 등 모든 권한
- `MANAGER` — 매치 생성, 가상 팀 등록 등 운영 권한
- `PLAYER` — 일반 팀원

### Match 엔티티 (테이블명: `match_record`)
| 필드 | 설명 |
|------|------|
| `homeTeam` | 매치를 생성한 팀 (실제 팀) |
| `opponentTeam` | 상대 팀 (실제 팀 또는 가상 팀) |
| `matchDate` | 경기 날짜/시간 |
| `location` | 경기 장소 |
| `createdBy` | 생성한 Member |

#### 매치 생성 규칙
- OWNER 또는 MANAGER만 생성 가능
- 상대 팀이 가상 팀인 경우, 내 팀이 만든 가상 팀이어야 함 (타 팀의 가상 팀 사용 불가)

## 인증 흐름 (JWT)

```
POST /api/auth/login { username, password }
  → 성공: { token: "Bearer ..." } 반환
  → 이후 모든 요청: Authorization: Bearer <token> 헤더 필요
```

## API 엔드포인트

### 인증
| 메서드 | URL | 권한 | 설명 |
|--------|-----|------|------|
| `POST` | `/api/auth/login` | 공개 | 로그인, JWT 발급 |

### 회원
| 메서드 | URL | 권한 | 설명 |
|--------|-----|------|------|
| `POST` | `/api/members/signup` | 공개 | 회원가입 |
| `GET` | `/api/members` | 인증 | 전체 회원 목록 |
| `GET` | `/api/members/{id}` | 인증 | 회원 상세 |
| `PUT` | `/api/members/{id}` | 인증(본인) | 회원 정보 수정 |

### 팀
| 메서드 | URL | 권한 | 설명 |
|--------|-----|------|------|
| `POST` | `/api/teams` | 인증 | 실제 팀 생성 (생성자 → OWNER) |
| `GET` | `/api/teams` | 인증 | 전체 팀 목록 |
| `GET` | `/api/teams/{id}` | 인증 | 팀 상세 |
| `PUT` | `/api/teams/{id}` | OWNER | 팀 이름 수정 |
| `DELETE` | `/api/teams/{id}` | OWNER | 팀 삭제 |
| `GET` | `/api/teams/search?name=xxx&myTeamId=yyy` | 인증 | 팀 이름 검색 (실제 팀 + 내 팀의 가상 팀) |
| `POST` | `/api/teams/virtual` | OWNER/MANAGER | 가상 팀 생성 |

### 매치
| 메서드 | URL | 권한 | 설명 |
|--------|-----|------|------|
| `POST` | `/api/matches` | OWNER/MANAGER | 매치 생성 |
| `GET` | `/api/matches?teamId=xxx` | 인증 | 팀의 매치 목록 |
| `GET` | `/api/matches/{id}` | 인증 | 매치 상세 |

## 공개 엔드포인트 (인증 불필요)
- `POST /api/auth/login`
- `POST /api/members/signup`

## DB 설정

- MySQL, 포트 3306, DB명 `jochuckhub`
- `spring.jpa.hibernate.ddl-auto=create-drop` (개발 환경)

## 파일 관리

- 새로 추가된 파일이 있으면 작업 후 git add 한다.
