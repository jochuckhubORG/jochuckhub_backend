# API 리뷰 및 반영 현황

기준일: 2026-07-25

## 상태 정의

- **리뷰 안함**: Codex의 컨트롤러 단위 리뷰 전
- **리뷰 완료 · 사용자 미검토**: 리뷰와 후속 수정은 진행됐고, 사용자의 최종 확인 전
- **사용자 검토 완료**: 사용자가 해당 컨트롤러 리뷰의 종료를 명시함

## AuthController — 사용자 검토 완료

- `GET /api/auth/kakao`, `GET /api/auth/kakao/callback`
- CSRF 토큰 쿠키(`XSRF-TOKEN`)와 요청 헤더 검증을 적용했다.
- 카카오 OAuth 요청 URI는 URI Builder로 만들고, 외부 API 요청·응답 및 오류를 로그로 남긴다.
- 카카오 액세스 토큰 저장 및 라인업 메시지 발송 기능은 제거했다.
- JWT 쿠키의 운영 `Secure=true` 전환은 TODO로 남겨 두었다.

## MemberController — 리뷰 완료 · 사용자 미검토

- `GET /api/members`는 최근 가입순, 페이지당 20건의 `PageResponse<MemberResponse>`를 반환한다.
- 회원 응답에서 `username`을 제거했다.
- 출석 점수와 골 기록은 팀 존재 여부 및 요청자 팀 소속을 검증한다.
- 골 기록은 잘못된 정렬·유형과 잘못된 날짜 범위를 400으로 처리하고, 페이지당 20건으로 조회한다.
- JPA Auditing으로 모든 주요 엔티티에 `createdAt`, `updatedAt`을 적용했다.

## TeamController — 리뷰 완료 · 사용자 미검토

- 실제 팀명은 전체 범위에서, 가상 팀명은 생성 팀 범위에서 DB 고유 키로 보장한다.
- 팀 삭제는 물리 삭제가 아닌 비활성화이며, 기존 경기 기록은 유지한다.
- `POST /api/teams/{id}/join`은 가입 요청을 만들고 즉시 가입시키지 않는다.
- `GET /api/teams/{id}/join-requests`와 `PATCH /api/teams/{id}/join-requests/{requestId}`로 OWNER/MANAGER가 요청을 조회하고 승인 또는 거절한다.
- 가상 팀 검색의 `myTeamId`와 팀원 통계 조회 모두 요청자 팀 소속을 검증한다.
- 내 팀 목록은 팀원 수를 포함한 DTO 프로젝션 집계 쿼리로, 팀원 통계는 조회 전용 DTO와 일괄 서브 포지션 조회로 N+1을 제거한다.

## MatchController — 리뷰 완료 · 사용자 미검토

- `POST /api/matches`, `GET /api/matches`, `GET /api/matches/{id}`, `PUT /api/matches/{id}/result`, `GET /api/matches/{id}/result`
- 홈팀과 상대팀의 동일성, 투표 마감 시각(현재 시각 이후·경기 한 시간 전 이내)을 검증한다.
- 매치 목록은 팀 존재·요청자 소속을 검증하고, DTO 프로젝션 집계가 아닌 단일 DTO 프로젝션 조회로 N+1을 제거한다.
- 단건 매치 조회는 홈팀 소속을 검증하고, 상세 연관관계를 fetch join으로 가져온다.
- 결과 입력은 지각과 무단불참 명단의 중복을 400으로 처리한다.
- 결과 조회는 매치 존재를 먼저 확인해 없는 매치에 `MATCH_NOT_FOUND` 404를 반환하고, 골·득점자·어시스트를 fetch join으로 조회한다.
- 경기 결과는 `@Version` 낙관적 락으로 보호한다. 프론트는 결과 조회 응답의 `version`을 결과 저장 요청에 포함하고, 충돌 시 `409 OPTIMISTIC_LOCK_CONFLICT`를 받아 새로고침 후 재시도한다.

## MatchVoteController — 리뷰 완료 · 사용자 미검토

- `POST /api/matches/{matchId}/votes`, `PUT /api/matches/{matchId}/votes`, `GET /api/matches/{matchId}/votes`, `PATCH /api/matches/{matchId}/votes/{memberId}/actual-status`
- 홈팀 구성원만 투표·투표 현황을 조회하고, OWNER/MANAGER만 경기 시작 후 실제 출석 상태를 기록한다.
- 없는 매치는 `MATCH_NOT_FOUND` 404로 처리한다.
- 투표 마감 시각부터 투표를 닫고, 투표 현황은 투표·팀원 이름 DTO 프로젝션으로 조회해 N+1을 제거한다.

## MatchLineupController — 리뷰 완료 · 사용자 미검토

- `POST /api/matches/{matchId}/lineup`, `PUT /api/matches/{matchId}/lineup`, `GET /api/matches/{matchId}/lineup`
- 자동 생성과 수동 저장은 OWNER/MANAGER 및 투표 마감 조건을 검증한다.
- 수동 저장은 선수의 홈팀 소속·ATTEND 투표 여부, 쿼터 내 선수 중복, 4-3-3 포메이션 슬롯을 검증한다.
- 라인업 조회는 홈팀 소속을 검증하고, 엔트리와 회원을 fetch join으로 조회한다. 부포지션은 `@BatchSize(size = 20)`을 사용한다.
- 자동 생성의 최근 8경기 점수는 현재 경기 이전의 완료된 경기만 일괄 집계한다.

## 검증

- `./gradlew.bat test` 성공
- 최신 반영 커밋: `8da26b0`, `7628f3c`

## 다음 확인

MemberController와 TeamController는 사용자가 검토 완료를 선언하면 `AGENTS.md`의 상태를 **사용자 검토 완료**로 변경한다.
