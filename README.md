# 조축허브(JochuckHub) 백엔드

조기 축구 팀의 회원, 경기, 참석 투표, 경기 결과와 라인업을 관리하는 모바일 애플리케이션용 REST API 서버입니다.

## 기술 스택

- Java 17
- Spring Boot 3.3.4
- Spring Security + JWT(Bearer 인증)
- Spring Data JPA, Querydsl, Hibernate Envers
- MySQL
- Gradle
- SpringDoc OpenAPI(Swagger)
- Resilience4j, Spring Actuator, Spring AOP

## 주요 기능

- Kakao SDK access token을 이용한 모바일 로그인
- 회전 가능한 refresh token과 짧은 수명의 JWT access token 발급
- 실제 팀 및 외부 상대를 위한 가상 팀 관리
- 팀 가입 요청과 `OWNER` / `MANAGER` / `PLAYER` 권한 관리
- 경기 생성, 참석 투표, 실제 출석 상태 및 참여 점수 관리
- 득점·도움·상대 팀 골과 경기 결과 기록
- 참석자 14~20명을 대상으로 한 4-3-3 라인업 자동 생성
- Hibernate Envers 기반 주요 엔티티 변경 이력 기록

## 인증 흐름

이 서버는 웹 브라우저의 인증 쿠키나 OAuth 콜백을 사용하지 않습니다. Flutter 등 모바일 앱이 Kakao SDK로 로그인한 후 카카오 access token을 백엔드에 전달합니다.

```text
모바일 앱에서 Kakao SDK 로그인
        ↓
카카오 access token 획득
        ↓
POST /api/auth/kakao
        ↓
조축허브 access token + refresh token 발급
        ↓
Authorization: Bearer {accessToken}
```

### 카카오 로그인

```http
POST /api/auth/kakao
Content-Type: application/json

{
  "kakaoAccessToken": "KAKAO_ACCESS_TOKEN"
}
```

응답 예시:

```json
{
  "accessToken": "SERVICE_ACCESS_TOKEN",
  "refreshToken": "SERVICE_REFRESH_TOKEN",
  "tokenType": "Bearer",
  "memberId": 1,
  "newMember": false
}
```

서버는 카카오 access token으로 카카오 사용자 정보 API를 호출해 사용자를 검증합니다. 이후 API에는 카카오 토큰이 아니라 서버가 발급한 조축허브 access token을 사용합니다.

### 인증 API 호출

```http
GET /api/members/me
Authorization: Bearer SERVICE_ACCESS_TOKEN
```

모바일 앱은 서비스 토큰을 iOS Keychain 또는 Android Keystore와 같은 OS 보안 저장소에 보관해야 합니다.

### 토큰 갱신

```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "SERVICE_REFRESH_TOKEN"
}
```

갱신에 성공하면 기존 refresh token은 폐기되고 새로운 access/refresh token 쌍이 반환됩니다.

### 로그아웃

```http
POST /api/auth/logout
Content-Type: application/json

{
  "refreshToken": "SERVICE_REFRESH_TOKEN"
}
```

로그아웃은 서버에 저장된 refresh token을 폐기합니다. 모바일 앱도 로컬 보안 저장소의 토큰을 함께 삭제해야 합니다.

## 로컬 실행

### 요구사항

- JDK 17
- MySQL 8
- 데이터베이스 `jochuckhub`

### 비밀 설정

Git에 포함되지 않는 `src/main/resources/application-private.properties`를 생성합니다.

```properties
spring.datasource.password=<mysql_password>
jwt.secret=<base64_encoded_secret_at_least_256_bits>
```

Kakao SDK 초기화에 사용하는 네이티브 앱 키와 Android/iOS 플랫폼 설정은 모바일 프로젝트 및 Kakao Developers에서 관리합니다. 백엔드는 Kakao SDK가 발급한 access token만 전달받으므로 카카오 Client Secret을 저장하지 않습니다.

### 실행 및 테스트

Windows:

```powershell
.\gradlew.bat bootRun
.\gradlew.bat test
```

macOS/Linux:

```bash
./gradlew bootRun
./gradlew test
```

서버 기본 주소는 `http://localhost:8080`입니다.

## API 문서

서버 실행 후 Swagger UI에서 전체 요청·응답 명세를 확인할 수 있습니다.

```text
http://localhost:8080/swagger-ui/index.html
```

대표 API:

| 영역 | 메서드 | 경로 | 권한 |
|---|---|---|---|
| 인증 | POST | `/api/auth/kakao` | 공개 |
| 인증 | POST | `/api/auth/refresh` | 공개 |
| 인증 | POST | `/api/auth/logout` | 공개 |
| 회원 | GET | `/api/members/me` | 인증 |
| 회원 | PUT | `/api/members/{id}` | 본인 |
| 팀 | POST | `/api/teams` | 인증 |
| 팀 | GET | `/api/teams` | 인증 |
| 팀 | POST | `/api/teams/{id}/join` | 인증 |
| 팀 | POST | `/api/teams/virtual` | OWNER/MANAGER |
| 경기 | POST | `/api/matches` | OWNER/MANAGER |
| 경기 | GET | `/api/matches?teamId={teamId}` | 팀원 |
| 결과 | PUT | `/api/matches/{id}/result` | OWNER/MANAGER |
| 투표 | POST | `/api/matches/{matchId}/votes` | 홈팀 팀원 |
| 라인업 | POST | `/api/matches/{matchId}/lineup` | OWNER/MANAGER |

세부 엔드포인트와 도메인 규칙은 [AGENTS.md](AGENTS.md), 컨트롤러별 리뷰 이력은 [docs/api-review-status.md](docs/api-review-status.md)를 참고하세요.

## 주요 도메인 규칙

- access token 유효기간: 15분
- refresh token 유효기간: 14일, 갱신 시 회전
- 실제 팀 이름은 전체에서 고유하며 가상 팀 이름은 생성 팀 내에서 고유
- 팀 삭제는 경기 기록을 보존하는 비활성화 방식
- 경기는 현재 시각 기준 최소 2시간 이후부터 생성 가능
- 경기 결과 저장은 `@Version` 기반 낙관적 락 사용
- 참석 투표 점수: 정상 참석 2점, 지각 1점, 불참 0점, 무단불참 -1점
- 라인업 자동 생성 참석 인원: 14~20명
- 지원 포지션: `GK`, `CB`, `LB`, `RB`, `CDM`, `CM`, `LW`, `RW`, `ST`

## 프로젝트 구조

```text
src/main/java/com/guenbon/jochuckhub/
├── config/       # Security, JWT, JPA, 로깅 설정
├── controller/   # REST API 컨트롤러
├── dto/          # 요청 및 응답 DTO
├── entity/       # JPA 엔티티와 enum
├── exception/    # 전역 예외 처리와 도메인 예외
├── repository/   # Spring Data JPA 및 Querydsl 저장소
└── service/      # 인증과 도메인 비즈니스 로직
```
