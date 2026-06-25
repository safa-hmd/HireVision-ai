package com.projet.hirevisionai.Repository;

import com.projet.hirevisionai.Entity.VoiceAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoiceAnalysisRepository extends JpaRepository<VoiceAnalysis, Long> {
    Optional<VoiceAnalysis> findByInterviewId(Long interviewId);
    boolean existsByInterviewId(Long interviewId);
    List<VoiceAnalysis> findByInterviewUserIdUser(Long userId);

    @Query("SELECT AVG(v.clarityScore) FROM VoiceAnalysis v " +
            "WHERE v.interview.user.idUser = :userId")
    Double findAvgClarityScoreByUserId(@Param("userId") Long userId);

    @Query("SELECT AVG(v.paceScore) FROM VoiceAnalysis v " +
            "WHERE v.interview.user.idUser = :userId")
    Double findAvgPaceScoreByUserId(@Param("userId") Long userId);
}