package com.projet.hirevisionai.Repository;

import com.projet.hirevisionai.Entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    Optional<Feedback> findByInterviewId(Long interviewId);
    boolean existsByInterviewId(Long interviewId);
    List<Feedback> findByInterviewUserIdUser(Long userId);

    @Query("SELECT AVG(f.technicalScore) FROM Feedback f " +
            "WHERE f.interview.user.idUser = :userId")
    Double findAvgTechnicalScoreByUserId(@Param("userId") Long userId);

    @Query("SELECT AVG(f.communicationScore) FROM Feedback f " +
            "WHERE f.interview.user.idUser = :userId")
    Double findAvgCommunicationScoreByUserId(@Param("userId") Long userId);

    @Query("SELECT AVG(f.confidenceScore) FROM Feedback f " +
            "WHERE f.interview.user.idUser = :userId")
    Double findAvgConfidenceScoreByUserId(@Param("userId") Long userId);
}