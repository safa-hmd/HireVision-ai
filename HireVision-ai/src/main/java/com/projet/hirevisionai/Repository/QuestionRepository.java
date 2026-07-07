package com.projet.hirevisionai.Repository;

import com.projet.hirevisionai.Entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByInterviewId(Long interviewId);
    List<Question> findByDifficulty(Question.Difficulty difficulty);
    List<Question> findByInterviewIdAndDifficulty(
            Long interviewId, Question.Difficulty difficulty);
    long countByInterviewId(Long interviewId);

    @Query("SELECT q FROM Question q WHERE q.interview.id = :interviewId " +
            "AND q.answer IS NULL")
    List<Question> findUnansweredByInterviewId(@Param("interviewId") Long interviewId);

    @Query("SELECT q.difficulty, COUNT(q) FROM Question q GROUP BY q.difficulty")
    List<Object[]> countGroupedByDifficulty();
}