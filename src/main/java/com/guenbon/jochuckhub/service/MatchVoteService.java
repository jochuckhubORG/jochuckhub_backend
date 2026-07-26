package com.guenbon.jochuckhub.service;

import com.guenbon.jochuckhub.dto.CustomUserDetails;
import com.guenbon.jochuckhub.dto.request.MatchVoteRequest;
import com.guenbon.jochuckhub.dto.request.UpdateActualStatusRequest;
import com.guenbon.jochuckhub.dto.response.MatchVoteResponse;
import com.guenbon.jochuckhub.dto.response.MatchVoteResultResponse;
import com.guenbon.jochuckhub.dto.response.MemberNameProjection;
import com.guenbon.jochuckhub.entity.ActualAttendStatus;
import com.guenbon.jochuckhub.entity.AttendStatus;
import com.guenbon.jochuckhub.entity.Match;
import com.guenbon.jochuckhub.entity.Member;
import com.guenbon.jochuckhub.entity.MatchVote;
import com.guenbon.jochuckhub.entity.TeamRole;
import com.guenbon.jochuckhub.exception.ForbiddenException;
import com.guenbon.jochuckhub.exception.MemberNotFoundException;
import com.guenbon.jochuckhub.exception.MatchNotFoundException;
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
            throw new IllegalArgumentException("A vote already exists. Use PUT to update it.");
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
                .orElseThrow(() -> new IllegalArgumentException("Vote not found. Create a vote first."));
        vote.updateStatus(request.getAttendStatus());
        return new MatchVoteResponse(vote);
    }

    public MatchVoteResultResponse getVoteResult(Long matchId, CustomUserDetails userDetails) {
        Match match = findMatch(matchId);
        checkHomeTeamMember(match, userDetails.getMemberId());

        List<MatchVoteResponse> allVotes = matchVoteRepository.findResponsesByMatchId(matchId);
        List<MemberNameProjection> teamMembers = teamMemberRepository
                .findMemberNamesByTeamId(match.getHomeTeam().getId());
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
        boolean isManager = teamMemberRepository.existsByTeamIdAndMemberIdAndRoleIn(
                match.getHomeTeam().getId(), requesterId,
                List.of(TeamRole.OWNER, TeamRole.MANAGER));
        if (!isManager) {
            throw new ForbiddenException("Only the team owner or manager can update actual attendance.");
        }
        if (LocalDateTime.now().isBefore(match.getMatchDate())) {
            throw new IllegalArgumentException("Actual attendance can be updated after the match starts.");
        }

        MatchVote vote = matchVoteRepository.findByMatchIdAndMemberId(matchId, targetMemberId)
                .orElseThrow(() -> new IllegalArgumentException("Vote not found for the target member."));
        if (!AttendStatus.ATTEND.equals(vote.getAttendStatus())) {
            throw new IllegalArgumentException("Actual attendance can only be set for attending voters.");
        }
        vote.updateActualStatus(request.getActualStatus());
        return new MatchVoteResponse(vote);
    }

    private Match findMatch(Long matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(MatchNotFoundException::new);
    }

    private void checkVoteOpen(Match match) {
        if (!LocalDateTime.now().isBefore(match.getEffectiveVoteDeadline())) {
            throw new IllegalArgumentException("Voting is closed.");
        }
    }

    private void checkHomeTeamMember(Match match, Long memberId) {
        Long homeTeamId = match.getHomeTeam().getId();
        if (!teamMemberRepository.existsByTeamIdAndMemberId(homeTeamId, memberId)) {
            throw new ForbiddenException("Only home team members can access votes.");
        }
    }
}
