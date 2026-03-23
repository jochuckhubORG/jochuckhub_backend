package com.guenbon.jochuckhub.repository;

import com.guenbon.jochuckhub.entity.MatchVote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MatchVoteRepository extends JpaRepository<MatchVote, Long> {

    Optional<MatchVote> findByMatchIdAndMemberId(Long matchId, Long memberId);

    List<MatchVote> findAllByMatchId(Long matchId);
}
