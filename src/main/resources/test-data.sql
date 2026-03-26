-- =========================================================
-- 자동 포지션 생성 기능 테스트 데이터
-- =========================================================
-- 주의: 실행 시 기존 데이터가 모두 삭제됩니다.
--
-- 로그인 정보 (비밀번호: 1234)
--   OWNER  : owner1
--   MANAGER: manager2, manager3
--   PLAYER : player4 ~ player20
--
-- 테스트 시나리오:
--   홈팀(우리팀FC, id=1) vs 상대팀FC(id=2)
--   테스트 매치 id=6, voteDeadline=2026-03-27 08:00 (이미 만료)
--   ATTEND 16명 → 팀원들 투표 완료 상태
--
--   [직접 테스트 순서]
--   1. POST /api/members/signup   → 본인 계정 생성
--   2. POST /api/teams/1/join     → 우리팀FC(id=1) 가입 (PLAYER)
--   3. DB 직접 수정:
--        UPDATE team_member SET role='MANAGER'
--        WHERE team_id=1 AND member_id=<본인 id>;
--   4. POST /api/matches/6/lineup → 자동 라인업 생성
--
-- N=16 기준:
--   3쿼터 배정 인원 x = 40 - 2*16 = 8명
--   2쿼터 배정 인원 y = 3*16 - 40 = 8명
-- =========================================================

SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE match_lineup_entry;
TRUNCATE TABLE goal;
TRUNCATE TABLE match_vote;
TRUNCATE TABLE match_record;
TRUNCATE TABLE team_member;
TRUNCATE TABLE member_sub_position;
TRUNCATE TABLE member;
TRUNCATE TABLE team;

SET FOREIGN_KEY_CHECKS = 1;

-- ===== 팀 =====
INSERT INTO team (id, name, is_virtual, created_by_team_id) VALUES
(1, '우리팀FC', false, NULL),
(2, '상대팀FC', false, NULL);

-- ===== 멤버 (20명) =====
-- BCrypt("1234") = $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
INSERT INTO member (id, username, name, password, main_position) VALUES
(1,  'owner1',    '김지훈', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ST'),
(2,  'manager2',  '이성민', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'CB'),
(3,  'manager3',  '박준호', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'CB'),
(4,  'player4',   '최동욱', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'LB'),
(5,  'player5',   '정하늘', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'RB'),
(6,  'player6',   '강민석', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'CM'),
(7,  'player7',   '윤성현', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'CM'),
(8,  'player8',   '조현준', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'CDM'),
(9,  'player9',   '서지훈', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'LW'),
(10, 'player10',  '한민준', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'RW'),
(11, 'player11',  '임동현', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ST'),
(12, 'player12',  '신재원', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'CB'),
(13, 'player13',  '오정수', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'LB'),
(14, 'player14',  '허민재', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'RB'),
(15, 'player15',  '송현우', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'CM'),
(16, 'player16',  '전준혁', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'CDM'),
(17, 'player17',  '황성진', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'LW'),
(18, 'player18',  '남기훈', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ST'),
(19, 'player19',  '류현준', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'RW'),
(20, 'player20',  '문성호', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'CB');

-- ===== 서브 포지션 =====
INSERT INTO member_sub_position (member_id, position) VALUES
-- 1번 김지훈 (ST): LW, RW
(1, 'LW'), (1, 'RW'),
-- 2번 이성민 (CB): CDM, LB
(2, 'CDM'), (2, 'LB'),
-- 3번 박준호 (CB): RB, LB
(3, 'RB'), (3, 'LB'),
-- 4번 최동욱 (LB): CB, CDM
(4, 'CB'), (4, 'CDM'),
-- 5번 정하늘 (RB): CB, CDM
(5, 'CB'), (5, 'CDM'),
-- 6번 강민석 (CM): CDM, LW
(6, 'CDM'), (6, 'LW'),
-- 7번 윤성현 (CM): LW, CDM
(7, 'LW'), (7, 'CDM'),
-- 8번 조현준 (CDM): CB, CM
(8, 'CB'), (8, 'CM'),
-- 9번 서지훈 (LW): ST, RW
(9, 'ST'), (9, 'RW'),
-- 10번 한민준 (RW): LW, ST
(10, 'LW'), (10, 'ST'),
-- 11번 임동현 (ST): LW, RW
(11, 'LW'), (11, 'RW'),
-- 12번 신재원 (CB): LB, RB
(12, 'LB'), (12, 'RB'),
-- 13번 오정수 (LB): CM, CB
(13, 'CM'), (13, 'CB'),
-- 14번 허민재 (RB): LB, CM
(14, 'LB'), (14, 'CM'),
-- 15번 송현우 (CM): RW, ST
(15, 'RW'), (15, 'ST'),
-- 16번 전준혁 (CDM): CM, LB
(16, 'CM'), (16, 'LB'),
-- 17번 황성진 (LW): CM, ST
(17, 'CM'), (17, 'ST'),
-- 18번 남기훈 (ST): CDM, RW
(18, 'CDM'), (18, 'RW'),
-- 19번 류현준 (RW): LW, CM
(19, 'LW'), (19, 'CM'),
-- 20번 문성호 (CB): LB, RB
(20, 'LB'), (20, 'RB');

-- ===== 팀원 (홈팀 전원 등록) =====
INSERT INTO team_member (id, team_id, member_id, role) VALUES
(1,  1, 1,  'OWNER'),
(2,  1, 2,  'MANAGER'),
(3,  1, 3,  'MANAGER'),
(4,  1, 4,  'PLAYER'),
(5,  1, 5,  'PLAYER'),
(6,  1, 6,  'PLAYER'),
(7,  1, 7,  'PLAYER'),
(8,  1, 8,  'PLAYER'),
(9,  1, 9,  'PLAYER'),
(10, 1, 10, 'PLAYER'),
(11, 1, 11, 'PLAYER'),
(12, 1, 12, 'PLAYER'),
(13, 1, 13, 'PLAYER'),
(14, 1, 14, 'PLAYER'),
(15, 1, 15, 'PLAYER'),
(16, 1, 16, 'PLAYER'),
(17, 1, 17, 'PLAYER'),
(18, 1, 18, 'PLAYER'),
(19, 1, 19, 'PLAYER'),
(20, 1, 20, 'PLAYER');

-- ===== 과거 매치 5개 (참여 점수 차별화용) =====
INSERT INTO match_record (id, home_team_id, opponent_team_id, match_date, location, created_by_id, vote_deadline, duration_minutes) VALUES
(1, 1, 2, '2026-01-10 10:00:00', '서울 월드컵공원', 1, '2026-01-09 10:00:00', 80),
(2, 1, 2, '2026-01-24 10:00:00', '서울 월드컵공원', 1, '2026-01-23 10:00:00', 80),
(3, 1, 2, '2026-02-07 10:00:00', '서울 월드컵공원', 1, '2026-02-06 10:00:00', 80),
(4, 1, 2, '2026-02-21 10:00:00', '서울 월드컵공원', 1, '2026-02-20 10:00:00', 80),
(5, 1, 2, '2026-03-07 10:00:00', '서울 월드컵공원', 1, '2026-03-06 10:00:00', 80);

-- ===== 테스트 매치 (id=6) =====
-- voteDeadline=2026-03-27 08:00 → 현재 시각 이전이므로 라인업 생성 가능
INSERT INTO match_record (id, home_team_id, opponent_team_id, match_date, location, created_by_id, vote_deadline, duration_minutes) VALUES
(6, 1, 2, '2026-03-28 14:00:00', '서울 월드컵공원', 1, '2026-03-27 08:00:00', 80);

-- ===== 과거 매치 투표 (참여 점수 반영) =====
-- 점수 분포: 멤버1~8 높음, 멤버9~14 중간, 멤버15~20 낮음
-- ATTEND=2점, ABSENT=0점, LATE=1점, NO_SHOW=-1점

-- 매치1 투표
INSERT INTO match_vote (id, match_id, member_id, attend_status, actual_status) VALUES
(1,  1, 1,  'ATTEND', NULL),
(2,  1, 2,  'ATTEND', NULL),
(3,  1, 3,  'ATTEND', NULL),
(4,  1, 4,  'ATTEND', NULL),
(5,  1, 5,  'ATTEND', NULL),
(6,  1, 6,  'ATTEND', NULL),
(7,  1, 7,  'ATTEND', NULL),
(8,  1, 8,  'ATTEND', NULL),
(9,  1, 9,  'ATTEND', NULL),
(10, 1, 10, 'ATTEND', NULL),
(11, 1, 11, 'ABSENT', NULL),
(12, 1, 12, 'ABSENT', NULL),
(13, 1, 13, 'ATTEND', 'LATE'),
(14, 1, 14, 'ATTEND', NULL),
(15, 1, 15, 'ABSENT', NULL),
(16, 1, 16, 'ATTEND', 'NO_SHOW'),
(17, 1, 17, 'ABSENT', NULL),
(18, 1, 18, 'ATTEND', NULL),
(19, 1, 19, 'ABSENT', NULL),
(20, 1, 20, 'ATTEND', NULL);

-- 매치2 투표
INSERT INTO match_vote (id, match_id, member_id, attend_status, actual_status) VALUES
(21, 2, 1,  'ATTEND', NULL),
(22, 2, 2,  'ATTEND', NULL),
(23, 2, 3,  'ATTEND', NULL),
(24, 2, 4,  'ATTEND', NULL),
(25, 2, 5,  'ATTEND', NULL),
(26, 2, 6,  'ATTEND', NULL),
(27, 2, 7,  'ATTEND', NULL),
(28, 2, 8,  'ATTEND', NULL),
(29, 2, 9,  'ABSENT', NULL),
(30, 2, 10, 'ATTEND', NULL),
(31, 2, 11, 'ATTEND', NULL),
(32, 2, 12, 'ABSENT', NULL),
(33, 2, 13, 'ATTEND', NULL),
(34, 2, 14, 'ABSENT', NULL),
(35, 2, 15, 'ATTEND', 'LATE'),
(36, 2, 16, 'ABSENT', NULL),
(37, 2, 17, 'ATTEND', NULL),
(38, 2, 18, 'ABSENT', NULL),
(39, 2, 19, 'ATTEND', NULL),
(40, 2, 20, 'ABSENT', NULL);

-- 매치3 투표
INSERT INTO match_vote (id, match_id, member_id, attend_status, actual_status) VALUES
(41, 3, 1,  'ATTEND', NULL),
(42, 3, 2,  'ATTEND', NULL),
(43, 3, 3,  'ATTEND', NULL),
(44, 3, 4,  'ATTEND', NULL),
(45, 3, 5,  'ATTEND', NULL),
(46, 3, 6,  'ATTEND', NULL),
(47, 3, 7,  'ABSENT', NULL),
(48, 3, 8,  'ATTEND', NULL),
(49, 3, 9,  'ATTEND', NULL),
(50, 3, 10, 'ABSENT', NULL),
(51, 3, 11, 'ATTEND', NULL),
(52, 3, 12, 'ATTEND', NULL),
(53, 3, 13, 'ABSENT', NULL),
(54, 3, 14, 'ATTEND', 'LATE'),
(55, 3, 15, 'ATTEND', NULL),
(56, 3, 16, 'ABSENT', NULL),
(57, 3, 17, 'ABSENT', NULL),
(58, 3, 18, 'ATTEND', 'NO_SHOW'),
(59, 3, 19, 'ABSENT', NULL),
(60, 3, 20, 'ATTEND', NULL);

-- 매치4 투표
INSERT INTO match_vote (id, match_id, member_id, attend_status, actual_status) VALUES
(61, 4, 1,  'ATTEND', NULL),
(62, 4, 2,  'ATTEND', NULL),
(63, 4, 3,  'ATTEND', NULL),
(64, 4, 4,  'ATTEND', NULL),
(65, 4, 5,  'ABSENT', NULL),
(66, 4, 6,  'ATTEND', NULL),
(67, 4, 7,  'ATTEND', NULL),
(68, 4, 8,  'ABSENT', NULL),
(69, 4, 9,  'ATTEND', NULL),
(70, 4, 10, 'ATTEND', NULL),
(71, 4, 11, 'ABSENT', NULL),
(72, 4, 12, 'ATTEND', NULL),
(73, 4, 13, 'ATTEND', NULL),
(74, 4, 14, 'ABSENT', NULL),
(75, 4, 15, 'ATTEND', NULL),
(76, 4, 16, 'ATTEND', 'LATE'),
(77, 4, 17, 'ATTEND', NULL),
(78, 4, 18, 'ABSENT', NULL),
(79, 4, 19, 'ATTEND', NULL),
(80, 4, 20, 'ABSENT', NULL);

-- 매치5 투표
INSERT INTO match_vote (id, match_id, member_id, attend_status, actual_status) VALUES
(81, 5, 1,  'ATTEND', NULL),
(82, 5, 2,  'ATTEND', NULL),
(83, 5, 3,  'ATTEND', NULL),
(84, 5, 4,  'ATTEND', NULL),
(85, 5, 5,  'ATTEND', NULL),
(86, 5, 6,  'ABSENT', NULL),
(87, 5, 7,  'ATTEND', NULL),
(88, 5, 8,  'ATTEND', NULL),
(89, 5, 9,  'ABSENT', NULL),
(90, 5, 10, 'ATTEND', NULL),
(91, 5, 11, 'ATTEND', NULL),
(92, 5, 12, 'ABSENT', NULL),
(93, 5, 13, 'ATTEND', NULL),
(94, 5, 14, 'ATTEND', NULL),
(95, 5, 15, 'ABSENT', NULL),
(96, 5, 16, 'ATTEND', NULL),
(97, 5, 17, 'ABSENT', NULL),
(98, 5, 18, 'ATTEND', NULL),
(99, 5, 19, 'ABSENT', NULL),
(100, 5, 20, 'ATTEND', NULL);

-- ===== 테스트 매치(6) 투표 =====
-- 멤버 1~16: ATTEND (16명) → 자동 라인업 생성 가능
-- 멤버 17~20: ABSENT (4명)
INSERT INTO match_vote (id, match_id, member_id, attend_status, actual_status) VALUES
(101, 6, 1,  'ATTEND', NULL),
(102, 6, 2,  'ATTEND', NULL),
(103, 6, 3,  'ATTEND', NULL),
(104, 6, 4,  'ATTEND', NULL),
(105, 6, 5,  'ATTEND', NULL),
(106, 6, 6,  'ATTEND', NULL),
(107, 6, 7,  'ATTEND', NULL),
(108, 6, 8,  'ATTEND', NULL),
(109, 6, 9,  'ATTEND', NULL),
(110, 6, 10, 'ATTEND', NULL),
(111, 6, 11, 'ATTEND', NULL),
(112, 6, 12, 'ATTEND', NULL),
(113, 6, 13, 'ATTEND', NULL),
(114, 6, 14, 'ATTEND', NULL),
(115, 6, 15, 'ATTEND', NULL),
(116, 6, 16, 'ATTEND', NULL),
(117, 6, 17, 'ABSENT', NULL),
(118, 6, 18, 'ABSENT', NULL),
(119, 6, 19, 'ABSENT', NULL),
(120, 6, 20, 'ABSENT', NULL);
