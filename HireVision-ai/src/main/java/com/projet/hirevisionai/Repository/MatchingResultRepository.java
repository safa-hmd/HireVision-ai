package com.projet.hirevisionai.Repository;

import com.projet.hirevisionai.Entity.MatchingResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchingResultRepository extends JpaRepository<MatchingResult, Long> {
    List<MatchingResult> findByCvId(Long cvId);
    List<MatchingResult> findByCvUserIdUser(Long userId);
    Optional<MatchingResult> findTopByCvIdOrderByScoreDesc(Long cvId);
    List<MatchingResult> findByCvIdAndScoreGreaterThanEqual(Long cvId, float minScore);
}