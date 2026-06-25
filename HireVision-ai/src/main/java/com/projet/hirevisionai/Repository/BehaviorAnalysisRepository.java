package com.projet.hirevisionai.Repository;

import com.projet.hirevisionai.Entity.BehaviorAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BehaviorAnalysisRepository extends JpaRepository<BehaviorAnalysis, Long> {
    Optional<BehaviorAnalysis> findByInterviewId(Long interviewId);
    boolean existsByInterviewId(Long interviewId);
    List<BehaviorAnalysis> findByInterviewUserIdUser(Long userId);

    @Query("SELECT AVG(b.postureScore) FROM BehaviorAnalysis b " +
            "WHERE b.interview.user.idUser = :userId")
    Double findAvgPostureScoreByUserId(@Param("userId") Long userId);

    @Query("SELECT AVG(b.eyeContactScore) FROM BehaviorAnalysis b " +
            "WHERE b.interview.user.idUser = :userId")
    Double findAvgEyeContactScoreByUserId(@Param("userId") Long userId);
}
