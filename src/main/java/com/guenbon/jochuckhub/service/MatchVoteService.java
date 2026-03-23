package com.guenbon.jochuckhub.service;

import com.guenbon.jochuckhub.dto.CustomUserDetails;
import com.guenbon.jochuckhub.dto.request.MatchVoteRequest;
import com.guenbon.jochuckhub.dto.request.UpdateActualStatusRequest;
import com.guenbon.jochuckhub.dto.response.MatchVoteResponse;
import com.guenbon.jochuckhub.dto.response.MatchVoteResultResponse;
import com.guenbon.jochuckhub.entity.AttendStatus;
import com.guenbon.jochuckhub.entity.Match;
import com.guenbon.jochuckhub.entity.Member;
import com.guenbon.jochuckhub.entity.MatchVote;
import com.guenbon.jochuckhub.entity.TeamRole;
import com.guenbon.jochuckhub.exception.ForbiddenException;
import com.guenbon.jochuckhub.exception.MemberNotFoundException;
import com.guenbon.jochuckhub.repository.MatchRepository;
import com.guenbon.jochuckhub.repository.MatchVoteRepository;
import com.guenbon.jochuckhub.repository.MemberRepository;
import com.guenbon.jochuckhub.repository.TeamMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchVoteService {

    private final MatchVoteRepository matchVoteRepository;
    private final MatchRepository matchRepository;
    private final MemberRepository memberRepository;
    private final TeamMemberRepository teamMemberRepository;

    @Transactional
    public MatchVoteResponse vote(Long matchId, MatchVoteRequest request, CustomUserDetails userDetails) {
        Match match = findMatch(matchId);
        checkVoteOpen(match);

        Long memberId = userDetails.getMemberId();
        checkHomeTeamMember(match, memberId);

        if (matchVoteRepository.findByMatchIdAndMemberId(matchId, memberId).isPresent()) {
            throw new IllegalArgumentException("이미 투표하셨습니다. 수정하려면 PUT 요청을 사용하세요.");
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);

        MatchVote vote = matchVoteRepository.save(MatchVote.builder()
                .match(match)
                .member(member)
                .attendStatus(request.getAttendStatus())
                .build());

        return new MatchVoteResponse(vote);
    }

    @Transactional
    public MatchVoteResponse updateVote(Long matchId, MatchVoteRequest request, CustomUserDetails userDetails) {
        Match match = findMatch(matchId);
        checkVoteOpen(match);

        Long memberId = userDetails.getMemberId();
        checkHomeTeamMember(match, memberId);

        MatchVote vote = matchVoteRepository.findByMatchIdAndMemberId(matchId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("투표 내역이 없습니다. 먼저 투표해주세요."));

        vote.updateStatus(request.getAttendStatus());
        return new MatchVoteResponse(vote);
    }

    public MatchVoteResultResponse getVoteResult(Long matchId, CustomUserDetails userDetails) {
        Match match = findMatch(matchId);
        Long memberId = userDetails.getMemberId();
        checkHomeTeamMember(match, memberId);

        List<MatchVoteResponse> allVotes = matchVoteRepository.findAllByMatchId(matchId).stream()
                .map(MatchVoteResponse::new)
                .toList();

        List<Member> teamMembers = teamMemberRepository.findAllByTeamId(match.getHomeTeam().getId())
                .stream()
                .map(tm -> tm.getMember())
                .toList();

        return new MatchVoteResultResponse(
                matchId,
                match.getEffectiveVoteDeadline(),
                match.getMatchDate(),
                allVotes,
                teamMembers
        );
    }

    @Transactional
    public MatchVoteResponse updateActualStatus(Long matchId, Long targetMemberId,
                                                UpdateActualStatusRequest request,
                                                CustomUserDetails userDetails) {
        Match match = findMatch(matchId);
        Long requesterId = userDetails.getMemberId();

        // OWNER 또는 MANAGER만 가능
        boolean isManager = teamMemberRepository.existsByTeamIdAndMemberIdAndRoleIn(
                match.getHomeTeam().getId(), requesterId,
                List.of(TeamRole.OWNER, TeamRole.MANAGER));
        if (!isManager) {
            throw new ForbiddenException("OWNER 또는 MANAGER만 실제 출석 상태를 변경할 수 있습니다.");
        }

        // 매치 시작 후에만 가능
        if (LocalDateTime.now().isBefore(match.getMatchDate())) {
            throw new IllegalArgumentException("매치 시작 후에만 실제 출석 상태를 변경할 수 있습니다.");
        }

        // ATTEND 투표자에게만 적용
        MatchVote vote = matchVoteRepository.findByMatchIdAndMemberId(matchId, targetMemberId)
                .orElseThrow(() -> new IllegalArgumentException("해당 멤버의 투표 내역이 없습니다."));

        if (!AttendStatus.ATTEND.equals(vote.getAttendStatus())) {
            throw new IllegalArgumentException("참석 투표한 멤버에게만 실제 출석 상태를 적용할 수 있습니다.");
        }

        vote.updateActualStatus(request.getActualStatus());
        return new MatchVoteResponse(vote);
    }

    private Match findMatch(Long matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("매치를 찾을 수 없습니다."));
    }

    private void checkVoteOpen(Match match) {
        if (LocalDateTime.now().isAfter(match.getEffectiveVoteDeadline())) {
            throw new IllegalArgumentException("투표가 종료되었습니다.");
        }
    }

    private void checkHomeTeamMember(Match match, Long memberId) {
        Long homeTeamId = match.getHomeTeam().getId();
        if (!teamMemberRepository.existsByTeamIdAndMemberId(homeTeamId, memberId)) {
            throw new ForbiddenException("홈 팀 소속 멤버만 접근할 수 있습니다.");
        }
    }
}
