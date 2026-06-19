package com.guenbon.jochuckhub.service;

import com.guenbon.jochuckhub.entity.*;
import com.guenbon.jochuckhub.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TestDataService {

    private static final String DUMMY_PREFIX = "TEST_DUMMY_";
    private static final int TARGET_ATTEND_COUNT = 14;

    private final MatchRepository matchRepository;
    private final MemberRepository memberRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final MatchVoteRepository matchVoteRepository;

    @Transactional
    public int setupLineupTest(Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("매치를 찾을 수 없습니다. id=" + matchId));

        List<MatchVote> currentAttendVotes =
                matchVoteRepository.findAllByMatchIdAndAttendStatus(matchId, AttendStatus.ATTEND);
        int needed = Math.max(0, TARGET_ATTEND_COUNT - currentAttendVotes.size());

        Position[] positions = Position.values();
        String prefix = DUMMY_PREFIX + matchId + "_";

        for (int i = 0; i < needed; i++) {
            final int idx = i;
            String kakaoId = prefix + idx;

            Member dummy = memberRepository.findByKakaoId(kakaoId)
                    .orElseGet(() -> memberRepository.save(
                            Member.builder()
                                    .kakaoId(kakaoId)
                                    .name("테스트팀원" + (idx + 1))
                                    .mainPosition(positions[idx % positions.length])
                                    .build()
                    ));

            if (!teamMemberRepository.existsByTeamIdAndMemberId(match.getHomeTeam().getId(), dummy.getId())) {
                teamMemberRepository.save(TeamMember.builder()
                        .team(match.getHomeTeam())
                        .member(dummy)
                        .role(TeamRole.PLAYER)
                        .build());
            }

            if (matchVoteRepository.findByMatchIdAndMemberId(matchId, dummy.getId()).isEmpty()) {
                matchVoteRepository.save(MatchVote.builder()
                        .match(match)
                        .member(dummy)
                        .attendStatus(AttendStatus.ATTEND)
                        .build());
            }
        }

        return needed;
    }

    @Transactional
    public int cleanupLineupTest(Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("매치를 찾을 수 없습니다. id=" + matchId));

        String prefix = DUMMY_PREFIX + matchId + "_";

        List<MatchVote> allVotes = matchVoteRepository.findAllByMatchId(matchId);

        List<Member> dummies = new ArrayList<>();
        for (MatchVote vote : allVotes) {
            if (vote.getMember().getKakaoId().startsWith(prefix)) {
                dummies.add(vote.getMember());
                matchVoteRepository.delete(vote);
            }
        }

        for (Member dummy : dummies) {
            teamMemberRepository.findByTeamIdAndMemberId(match.getHomeTeam().getId(), dummy.getId())
                    .ifPresent(teamMemberRepository::delete);
            memberRepository.delete(dummy);
        }

        return dummies.size();
    }
}
