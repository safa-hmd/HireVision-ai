package com.projet.hirevisionai.Repository;

import com.projet.hirevisionai.Entity.LearningPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LearningPlanRepository extends JpaRepository<LearningPlan, Long> {
    List<LearningPlan> findByMissedSkillId(Long missedSkillId);
    List<LearningPlan> findByMissedSkillMatchingResultCvUserIdUser(Long userId);
    List<LearningPlan> findByTitleContainingIgnoreCase(String keyword);

    // Plans issus des entretiens (source = INTERVIEW)
    List<LearningPlan> findByInterviewId(Long interviewId);
    List<LearningPlan> findByInterviewUserIdUser(Long userId);
    void deleteByInterviewId(Long interviewId);
}