package com.guenbon.jochuckhub.service;

import com.guenbon.jochuckhub.dto.CustomUserDetails;
import com.guenbon.jochuckhub.dto.request.UpdateMemberRequest;
import com.guenbon.jochuckhub.dto.response.MemberResponse;
import com.guenbon.jochuckhub.dto.response.PageResponse;
import com.guenbon.jochuckhub.entity.Member;
import com.guenbon.jochuckhub.exception.ForbiddenException;
import com.guenbon.jochuckhub.exception.MemberNotFoundException;
import com.guenbon.jochuckhub.exception.TeamNotFoundException;
import com.guenbon.jochuckhub.repository.MatchVoteRepository;
import com.guenbon.jochuckhub.repository.MemberRepository;
import com.guenbon.jochuckhub.repository.TeamMemberRepository;
import com.guenbon.jochuckhub.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private static final int PAGE_SIZE = 20;

    private final MemberRepository memberRepository;
    private final MatchVoteRepository matchVoteRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamRepository teamRepository;

    @Transactional
    public MemberResponse updateMember(Long targetId, UpdateMemberRequest request, CustomUserDetails requester) {
        if (!requester.getMemberId().equals(targetId)) {
            throw new ForbiddenException("자신의 정보만 수정할 수 있습니다.");
        }

        Member target = memberRepository.findById(targetId)
                .orElseThrow(MemberNotFoundException::new);
        if (request.getSubPositions().contains(request.getMainPosition())) {
            throw new IllegalArgumentException("주 포지션과 서브 포지션은 중복될 수 없습니다.");
        }

        target.update(request.getName(), request.getMainPosition(), request.getSubPositions());
        return new MemberResponse(target);
    }

    public MemberResponse getMember(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(MemberNotFoundException::new);
        return new MemberResponse(member);
    }

    public PageResponse<MemberResponse> getMembers(int page) {
        return PageResponse.from(
                memberRepository.findAllBy(PageRequest.of(page, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"))),
                MemberResponse::new
        );
    }

    public int getAttendanceScore(Long memberId, Long teamId, Long requesterId) {
        if (!teamRepository.existsByIdAndDeletedFalse(teamId)) {
            throw new TeamNotFoundException();
        }
        if (!teamMemberRepository.existsByTeamIdAndMemberId(teamId, requesterId)) {
            throw new ForbiddenException("해당 팀 소속 멤버만 출석 점수를 조회할 수 있습니다.");
        }
        if (!memberRepository.existsById(memberId)) {
            throw new MemberNotFoundException();
        }
        if (!teamMemberRepository.existsByTeamIdAndMemberId(teamId, memberId)) {
            throw new ForbiddenException("해당 팀 소속 멤버가 아닙니다.");
        }
        return matchVoteRepository
                .findTop8ByMemberIdAndMatchHomeTeamIdOrderByMatchMatchDateDesc(memberId, teamId)
                .stream()
                .mapToInt(vote -> vote.getScore())
                .sum();
    }
}
