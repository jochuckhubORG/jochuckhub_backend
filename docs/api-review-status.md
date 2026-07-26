# API 리뷰 및 반영 현황

기준일: 2026-07-26

이 문서는 테스트 전용 `TestDataController`를 제외한 서비스 API의 리뷰 상태와 반영 이력을 관리한다.

## 상태 기준

- `리뷰 안함`: 컨트롤러 단위 리뷰를 아직 제공하지 않음
- `리뷰 완료 · 사용자 미검토`: 리뷰와 코드 반영은 끝났으나, 사용자의 최종 확인 선언이 없음
- `사용자 검토 완료`: 사용자가 해당 컨트롤러의 리뷰를 확인하고 종료를 선언함

## 최종 교차 리뷰 결과

2026-07-26에 `AuthController`, `MemberController`, `TeamController`, `MatchController`, `MatchVoteController`, `MatchLineupController`를 다시 교차 점검했다. 서비스 API 33개 모두 최초 리뷰와 사용자가 요청한 수정 반영까지 완료되어 있다.

| 컨트롤러 | API 수 | 상태 | 최종 점검 결과 |
|---|---:|---|---|
| `AuthController` | 4 | 사용자 검토 완료 | CSRF 쿠키/헤더 검증, OAuth state 검증, URI 인코딩, 카카오 외부 API 오류 분류·로그, JWT/refresh token 보안 로그 반영 |
| `MemberController` | 6 | 사용자 검토 완료 | DTO 응답, 20건 페이지네이션, 팀 소속 검증, 입력값·날짜 범위 검증 반영 |
| `TeamController` | 11 | 사용자 검토 완료 | 이름 고유 제약, 가입 요청 승인, 소속 검증, DTO 프로젝션, 비활성화 삭제 반영 |
| `MatchController` | 5 | 사용자 검토 완료 | 팀 소속 검증, 자기 팀 경기/투표 마감 검증, 결과 조회 최적화, 낙관적 락 반영 |
| `MatchVoteController` | 4 | 사용자 검토 완료 | 없는 경기 404, 마감 시각 경계 처리, 투표 결과 DTO 프로젝션 반영 |
| `MatchLineupController` | 3 | 사용자 검토 완료 | 팀 소속·참석자·포메이션 검증, 일괄 최근 점수 조회, 회원 fetch join 반영 |

`TestDataController`의 2개 API는 테스트 지원 용도이므로 본 리뷰 범위에서 제외했다.

## 컨트롤러별 코드 추적 기준

| 컨트롤러 | 기본 추적 순서 |
|---|---|
| Auth | `AuthController` → `KakaoAuthService` → `MemberRepository` / `JwtTokenProvider` → `SecurityConfig` / JWT 필터 |
| Member | `MemberController` → `MemberService` → Member·Goal·MatchVote Repository → 응답 DTO / `PageResponse` |
| Team | `TeamController` → `TeamService` → Team·TeamMember·JoinRequest Repository → Team/TeamMember 엔티티와 DTO 프로젝션 |
| Match | `MatchController` → `MatchService` / `MatchResultService` → Match·Goal Repository → Match·Goal 엔티티와 결과 DTO |
| MatchVote | `MatchVoteController` → `MatchVoteService` → MatchVote Repository → 투표 집계 DTO / 팀 권한 검증 |
| MatchLineup | `MatchLineupController` → `MatchLineupService` → LineupEntry·MatchVote Repository → 라인업 엔티티·포지션 검증 |

## 공통 검증 사항

- 인증 필요 API는 Spring Security의 JWT 인증을 거치며, 쿠키 기반 상태 변경 요청에는 `XSRF-TOKEN` 쿠키와 요청 헤더를 비교하는 CSRF 방어가 적용되어 있다.
- 권한이 필요한 팀·경기·투표·라인업 API는 요청자의 해당 팀 소속 및 OWNER/MANAGER 역할을 서비스 계층에서 검증한다.
- `MEMBER_NOT_FOUND`, `TEAM_NOT_FOUND`, `MATCH_NOT_FOUND`, `FORBIDDEN`, 검증 오류, DB 충돌 및 낙관적 락 충돌이 일관된 오류 응답으로 변환된다.
- 카카오 외부 API 호출은 정상 완료·실패·네트워크 오류를 구분해 로그로 남기며, 토큰과 인가 코드 같은 비밀값은 로그에 남기지 않는다.

## JWT 발급·검증 재점검

- 발급 흐름은 `AuthController` → `KakaoAuthService` → `RefreshTokenService` → `JwtTokenProvider.generateAccessToken()`이다. 액세스 JWT에는 subject, 발급 시각, 만료 시각, `token_type=access`가 서명되어 15분 동안 `accessToken` HttpOnly 쿠키로 전달된다.
- 리프레시 토큰은 14일 유효한 512비트 난수이며 평문 대신 SHA-256 해시만 DB에 저장한다. `POST /api/auth/refresh`는 DB 행 잠금으로 동시 재발급을 막고, 기존 토큰을 삭제한 뒤 새 토큰으로 회전한다.
- 검증 흐름은 `JwtAuthenticationFilter` → `JwtTokenProvider.validateAndGetUsername()` → `CustomUserDetailsService`다. 서명·형식·만료 오류는 SecurityContext를 비우고 보안 이벤트 로그를 남긴 뒤, 보호 API에서 401로 처리한다.
- 로그아웃은 refresh token DB 레코드와 두 인증 쿠키를 모두 삭제한다. 강제 로그아웃이 필요할 때는 해당 회원의 refresh token 레코드를 제거하면 다음 액세스 토큰 만료 뒤 재발급을 차단할 수 있다.
- JWT 서명 키는 `jwt.secret`의 Base64 값을 디코딩해 사용한다. 기존 문자열 바이트 방식으로 발급한 access token은 새 배포 후 유효하지 않으므로 사용자는 한 번 다시 로그인해야 한다.

## 남은 개선 항목

- 카카오 OAuth 시작 요청은 256비트 난수 `state`를 HttpOnly·SameSite=Lax 쿠키에 5분 동안 저장하고, 콜백의 `state`와 상수 시간 비교로 검증한다. 불일치·누락 시 로그인을 중단하고 쿠키를 즉시 제거한다.
- 운영 배포 전에는 두 인증 쿠키의 `secure=true` 전환, 새 refresh token 테이블·감사 컬럼·팀 관련 컬럼의 운영 DB 마이그레이션, 경기 결과 수정 화면의 `version` 전송을 확인해야 한다.

## 검증 및 반영 커밋

- `./gradlew.bat test`를 각 수정 묶음 이후 실행해 성공을 확인했다. OAuth `state` 반영 후에도 2026-07-26에 성공을 확인했다.
- 주요 반영 커밋: `a9aa232` (인증/CSRF), `ebf38bc`·`a8cf37b` (회원/감사), `8da26b0`·`7628f3c` (팀), `3a3bb8b` (경기/결과), `104e25b` (투표), `be0627d` (라인업).

## 사용자 최종 확인 대기

테스트 전용 `TestDataController`를 제외한 모든 서비스 컨트롤러는 사용자 검토 완료 상태다.
