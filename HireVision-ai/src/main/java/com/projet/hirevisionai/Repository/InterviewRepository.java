package com.projet.hirevisionai.Repository;

import com.projet.hirevisionai.Entity.Interview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InterviewRepository extends JpaRepository<Interview, Long> {
    List<Interview> findByUserIdUser(Long userId);
    List<Interview> findByCvId(Long cvId);
    List<Interview> findByUserIdUserOrderByStartDateDesc(Long userId);
    long countByUserIdUser(Long userId);

    @Query("SELECT AVG(f.technicalScore) FROM Interview i " +
            "JOIN i.feedback f WHERE i.user.idUser = :userId")
    Double findAverageTechnicalScoreByUserId(@Param("userId") Long userId);

    @Query("SELECT i FROM Interview i WHERE i.user.idUser = :userId " +
            "AND i.startDate BETWEEN :start AND :end")
    List<Interview> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    long countByStartDateBetween(LocalDateTime start, LocalDateTime end);
    List<Interview> findTop5ByOrderByStartDateDesc();
    List<Interview> findAllByOrderByStartDateDesc();

    @Query("SELECT AVG((f.technicalScore + f.communicationScore + f.confidenceScore + f.eyeContactScore) / 4) " +
            "FROM Interview i JOIN i.feedback f WHERE i.startDate BETWEEN :start AND :end")
    Double findAvgScoreByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}