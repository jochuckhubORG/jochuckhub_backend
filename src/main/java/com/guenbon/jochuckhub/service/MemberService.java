package com.guenbon.jochuckhub.service;

import com.guenbon.jochuckhub.dto.CustomUserDetails;
import com.guenbon.jochuckhub.dto.request.SignUpRequest;
import com.guenbon.jochuckhub.dto.request.UpdateMemberRequest;
import com.guenbon.jochuckhub.dto.response.MemberResponse;
import com.guenbon.jochuckhub.entity.Member;
import com.guenbon.jochuckhub.exception.ForbiddenException;
import com.guenbon.jochuckhub.exception.MemberNotFoundException;
import com.guenbon.jochuckhub.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

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

    public List<MemberResponse> getMembers() {
        return memberRepository.findAll().stream()
                .map(MemberResponse::new)
                .toList();
    }

    @Transactional
    public void register(SignUpRequest request) {
        if (memberRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }

        if (request.getSubPositions().contains(request.getMainPosition())) {
            throw new IllegalArgumentException("주 포지션과 서브 포지션은 중복될 수 없습니다.");
        }

        Member member = Member.builder()
                .username(request.getUsername())
                .name(request.getName())
                .password(passwordEncoder.encode(request.getPassword()))
                .mainPosition(request.getMainPosition())
                .subPositions(request.getSubPositions())
                .build();

        memberRepository.save(member);
    }
}
