package com.projet.hirevisionai.Repository;

import com.projet.hirevisionai.Entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {
    Optional<Answer> findByQuestionId(Long questionId);
    boolean existsByQuestionId(Long questionId);
    List<Answer> findByQuestionInterviewId(Long interviewId);

    @Query("SELECT AVG(a.score) FROM Answer a " +
            "WHERE a.question.interview.id = :interviewId")
    Double findAverageScoreByInterviewId(@Param("interviewId") Long interviewId);

    @Query("SELECT AVG(a.score) FROM Answer a " +
            "WHERE a.question.interview.user.idUser = :userId")
    Double findAverageScoreByUserId(@Param("userId") Long userId);
}