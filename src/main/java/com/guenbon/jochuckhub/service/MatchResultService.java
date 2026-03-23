package com.guenbon.jochuckhub.service;

import com.guenbon.jochuckhub.dto.request.GoalRequest;
import com.guenbon.jochuckhub.dto.request.RecordMatchResultRequest;
import com.guenbon.jochuckhub.dto.response.MatchResultResponse;
import com.guenbon.jochuckhub.entity.*;
import com.guenbon.jochuckhub.exception.ForbiddenException;
import com.guenbon.jochuckhub.exception.MemberNotFoundException;
import com.guenbon.jochuckhub.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchResultService {

    private final MatchRepository matchRepository;
    private final GoalRepository goalRepository;
    private final MatchVoteRepository matchVoteRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final MemberRepository memberRepository;
    private final TeamService teamService;

    @Transactional
    public MatchResultResponse recordResult(Long matchId, RecordMatchResultRequest request, Long requesterId) {
        Match match = findMatch(matchId);
        teamService.verifyOwnerOrManager(match.getHomeTeam().getId(), requesterId);

        if (LocalDateTime.now().isBefore(match.getMatchEndTime())) {
            throw new IllegalArgumentException("경기가 아직 종료되지 않았습니다.");
        }

        // 홈팀 전체 멤버 ID
        Set<Long> homeTeamMemberIds = teamMemberRepository.findAllByTeamId(match.getHomeTeam().getId())
                .stream().map(tm -> tm.getMember().getId()).collect(Collectors.toSet());

        // ATTEND 투표자 맵 (memberId → MatchVote)
        Map<Long, MatchVote> attendVoteMap = matchVoteRepository
                .findAllByMatchIdAndAttendStatus(matchId, AttendStatus.ATTEND)
                .stream().collect(Collectors.toMap(v -> v.getMember().getId(), v -> v));

        Set<Long> lateIds = Set.copyOf(request.getLateMemberIds());
        Set<Long> noShowIds = Set.copyOf(request.getNoShowMemberIds());

        // late/no-show는 반드시 ATTEND 투표자여야 함
        for (Long id : lateIds) {
            if (!attendVoteMap.containsKey(id)) {
                throw new IllegalArgumentException("지각 처리 대상(" + id + ")은 ATTEND 투표자여야 합니다.");
            }
        }
        for (Long id : noShowIds) {
            if (!attendVoteMap.containsKey(id)) {
                throw new IllegalArgumentException("무단불참 처리 대상(" + id + ")은 ATTEND 투표자여야 합니다.");
            }
        }

        // 기존 골 삭제 (수정 시나리오)
        goalRepository.deleteAllByMatchId(matchId);

        // 골 저장
        List<Goal> savedGoals = request.getGoals().stream()
                .map(gr -> buildGoal(match, gr, attendVoteMap.keySet(), noShowIds))
                .map(goalRepository::save)
                .toList();

        // ATTEND 투표자 실제 출석 상태 반영
        for (Map.Entry<Long, MatchVote> entry : attendVoteMap.entrySet()) {
            Long memberId = entry.getKey();
            MatchVote vote = entry.getValue();
            if (noShowIds.contains(memberId)) {
                vote.updateActualStatus(ActualAttendStatus.NO_SHOW);
            } else if (lateIds.contains(memberId)) {
                vote.updateActualStatus(ActualAttendStatus.LATE);
            } else {
                vote.updateActualStatus(null); // 정상 참석 (null = 참석)
            }
        }

        // 투표하지 않은 홈팀 멤버 → ABSENT 투표 자동 생성
        Set<Long> alreadyVotedIds = matchVoteRepository.findAllByMatchId(matchId)
                .stream().map(v -> v.getMember().getId()).collect(Collectors.toSet());

        for (Long memberId : homeTeamMemberIds) {
            if (!alreadyVotedIds.contains(memberId)) {
                Member member = memberRepository.findById(memberId)
                        .orElseThrow(MemberNotFoundException::new);
                matchVoteRepository.save(MatchVote.builder()
                        .match(match)
                        .member(member)
                        .attendStatus(AttendStatus.ABSENT)
                        .build());
            }
        }

        return new MatchResultResponse(matchId, savedGoals);
    }

    public MatchResultResponse getResult(Long matchId) {
        return new MatchResultResponse(matchId, goalRepository.findAllByMatchId(matchId));
    }

    private Goal buildGoal(Match match, GoalRequest gr, Set<Long> attendVoterIds, Set<Long> noShowIds) {
        if (gr.isOpponentGoal()) {
            return Goal.builder().match(match).opponentGoal(true).build();
        }

        if (gr.getScorerMemberId() == null) {
            throw new IllegalArgumentException("홈팀 골의 경우 득점자(scorerMemberId)는 필수입니다.");
        }
        if (!attendVoterIds.contains(gr.getScorerMemberId())) {
            throw new IllegalArgumentException("득점자(" + gr.getScorerMemberId() + ")는 ATTEND 투표자여야 합니다.");
        }
        if (noShowIds.contains(gr.getScorerMemberId())) {
            throw new IllegalArgumentException("무단불참 처리된 멤버(" + gr.getScorerMemberId() + ")는 득점자가 될 수 없습니다.");
        }

        Member scorer = memberRepository.findById(gr.getScorerMemberId())
                .orElseThrow(MemberNotFoundException::new);

        Member assister = null;
        if (gr.getAssisterMemberId() != null) {
            if (!attendVoterIds.contains(gr.getAssisterMemberId())) {
                throw new IllegalArgumentException("어시스트(" + gr.getAssisterMemberId() + ")는 ATTEND 투표자여야 합니다.");
            }
            if (noShowIds.contains(gr.getAssisterMemberId())) {
                throw new IllegalArgumentException("무단불참 처리된 멤버(" + gr.getAssisterMemberId() + ")는 어시스트가 될 수 없습니다.");
            }
            assister = memberRepository.findById(gr.getAssisterMemberId())
                    .orElseThrow(MemberNotFoundException::new);
        }

        return Goal.builder().match(match).opponentGoal(false).scorer(scorer).assister(assister).build();
    }

    private Match findMatch(Long matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("매치를 찾을 수 없습니다."));
    }
}
