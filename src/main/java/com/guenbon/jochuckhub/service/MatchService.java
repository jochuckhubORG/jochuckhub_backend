package com.guenbon.jochuckhub.service;

import com.guenbon.jochuckhub.dto.CustomUserDetails;
import com.guenbon.jochuckhub.dto.request.CreateMatchRequest;
import com.guenbon.jochuckhub.dto.response.MatchResponse;
import com.guenbon.jochuckhub.entity.Match;
import com.guenbon.jochuckhub.entity.Member;
import com.guenbon.jochuckhub.entity.Team;
import com.guenbon.jochuckhub.exception.MemberNotFoundException;
import com.guenbon.jochuckhub.exception.TeamNotFoundException;
import com.guenbon.jochuckhub.repository.MatchRepository;
import com.guenbon.jochuckhub.repository.MemberRepository;
import com.guenbon.jochuckhub.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchService {

    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;
    private final MemberRepository memberRepository;
    private final TeamService teamService;

    @Transactional
    public MatchResponse createMatch(CreateMatchRequest request, CustomUserDetails requester) {
        Long homeTeamId = request.getHomeTeamId();

        teamService.verifyOwnerOrManager(homeTeamId, requester.getMemberId());

        Team homeTeam = teamRepository.findById(homeTeamId)
                .orElseThrow(TeamNotFoundException::new);

        Team opponentTeam = teamRepository.findById(request.getOpponentTeamId())
                .orElseThrow(TeamNotFoundException::new);

        // 가상 팀인 경우 내 팀이 만든 가상 팀인지 확인
        if (opponentTeam.isVirtual() && !homeTeamId.equals(opponentTeam.getCreatedByTeamId())) {
            throw new IllegalArgumentException("다른 팀이 만든 가상 팀은 상대로 선택할 수 없습니다.");
        }

        Member creator = memberRepository.findById(requester.getMemberId())
                .orElseThrow(MemberNotFoundException::new);

        Match match = matchRepository.save(Match.builder()
                .homeTeam(homeTeam)
                .opponentTeam(opponentTeam)
                .matchDate(request.getMatchDate())
                .location(request.getLocation())
                .createdBy(creator)
                .build());

        return new MatchResponse(match);
    }

    public List<MatchResponse> getMatchesByTeam(Long teamId) {
        return matchRepository.findAllByTeamId(teamId).stream()
                .map(MatchResponse::new)
                .toList();
    }

    public MatchResponse getMatch(Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("매치를 찾을 수 없습니다."));
        return new MatchResponse(match);
    }
}
