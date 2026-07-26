package com.guenbon.jochuckhub.service;

import com.guenbon.jochuckhub.dto.CustomUserDetails;
import com.guenbon.jochuckhub.dto.request.CreateMatchRequest;
import com.guenbon.jochuckhub.dto.response.MatchResponse;
import com.guenbon.jochuckhub.entity.Match;
import com.guenbon.jochuckhub.entity.Member;
import com.guenbon.jochuckhub.entity.Team;
import com.guenbon.jochuckhub.exception.ForbiddenException;
import com.guenbon.jochuckhub.exception.MemberNotFoundException;
import com.guenbon.jochuckhub.exception.MatchNotFoundException;
import com.guenbon.jochuckhub.exception.TeamNotFoundException;
import com.guenbon.jochuckhub.repository.MatchRepository;
import com.guenbon.jochuckhub.repository.MemberRepository;
import com.guenbon.jochuckhub.repository.TeamMemberRepository;
import com.guenbon.jochuckhub.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchService {

    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final MemberRepository memberRepository;
    private final TeamService teamService;

    @Transactional
    public MatchResponse createMatch(CreateMatchRequest request, CustomUserDetails requester) {
        Long homeTeamId = request.getHomeTeamId();
        if (homeTeamId.equals(request.getOpponentTeamId())) {
            throw new IllegalArgumentException("Home team and opponent team must be different.");
        }
        teamService.verifyOwnerOrManager(homeTeamId, requester.getMemberId());

        LocalDateTime now = LocalDateTime.now();
        if (request.getMatchDate().isBefore(now.plusHours(2))) {
            throw new IllegalArgumentException("Matches must be created at least two hours in advance.");
        }
        validateVoteDeadline(request, now);

        Team homeTeam = findActiveTeam(homeTeamId);
        Team opponentTeam = findActiveTeam(request.getOpponentTeamId());
        if (opponentTeam.isVirtual() && !homeTeamId.equals(opponentTeam.getCreatedByTeamId())) {
            throw new IllegalArgumentException("Only virtual teams created by the home team can be selected.");
        }

        Member creator = memberRepository.findById(requester.getMemberId())
                .orElseThrow(MemberNotFoundException::new);

        Match match = matchRepository.save(Match.builder()
                .homeTeam(homeTeam)
                .opponentTeam(opponentTeam)
                .matchDate(request.getMatchDate())
                .location(request.getLocation())
                .createdBy(creator)
                .voteDeadline(request.getVoteDeadline())
                .durationMinutes(request.getDurationMinutes())
                .build());
        return new MatchResponse(match);
    }

    public List<MatchResponse> getMatchesByTeam(Long teamId, Long requesterId) {
        findActiveTeam(teamId);
        verifyTeamMember(teamId, requesterId);
        return matchRepository.findResponsesByTeamId(teamId);
    }

    public MatchResponse getMatch(Long matchId, Long requesterId) {
        Match match = matchRepository.findByIdWithDetails(matchId)
                .orElseThrow(MatchNotFoundException::new);
        verifyTeamMember(match.getHomeTeam().getId(), requesterId);
        return new MatchResponse(match);
    }

    private Team findActiveTeam(Long teamId) {
        return teamRepository.findByIdAndDeletedFalse(teamId)
                .orElseThrow(TeamNotFoundException::new);
    }

    private void verifyTeamMember(Long teamId, Long memberId) {
        if (!teamMemberRepository.existsByTeamIdAndMemberId(teamId, memberId)) {
            throw new ForbiddenException("Only team members can view match information.");
        }
    }

    private void validateVoteDeadline(CreateMatchRequest request, LocalDateTime now) {
        LocalDateTime voteDeadline = request.getVoteDeadline();
        if (voteDeadline == null) {
            return;
        }
        LocalDateTime latestAllowedDeadline = request.getMatchDate().minusHours(1);
        if (!voteDeadline.isAfter(now) || voteDeadline.isAfter(latestAllowedDeadline)) {
            throw new IllegalArgumentException("voteDeadline must be after now and no later than one hour before the match.");
        }
    }
}
