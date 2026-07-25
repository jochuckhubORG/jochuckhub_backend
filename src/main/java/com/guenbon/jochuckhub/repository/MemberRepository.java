package com.guenbon.jochuckhub.repository;

import com.guenbon.jochuckhub.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByUsername(String username);
    Optional<Member> findByKakaoId(String kakaoId);

    Page<Member> findAllBy(Pageable pageable);
}
