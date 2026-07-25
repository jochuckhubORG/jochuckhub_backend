package com.guenbon.jochuckhub.repository;

import com.guenbon.jochuckhub.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByUsername(String username);

    Optional<Member> findByKakaoId(String kakaoId);

    Page<Member> findAllBy(Pageable pageable);

    @Query("SELECT m.id, p FROM Member m LEFT JOIN m.subPositions p WHERE m.id IN :memberIds")
    List<Object[]> findSubPositionsByMemberIds(@Param("memberIds") List<Long> memberIds);
}
