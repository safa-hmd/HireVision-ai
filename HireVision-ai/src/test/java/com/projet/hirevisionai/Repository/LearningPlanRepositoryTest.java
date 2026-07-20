package com.projet.hirevisionai.Repository;

import com.projet.hirevisionai.Entity.CV;
import com.projet.hirevisionai.Entity.Interview;
import com.projet.hirevisionai.Entity.JobOffer;
import com.projet.hirevisionai.Entity.LearningPlan;
import com.projet.hirevisionai.Entity.MatchingResult;
import com.projet.hirevisionai.Entity.MissedSkill;
import com.projet.hirevisionai.Entity.PlanSource;
import com.projet.hirevisionai.Entity.Role;
import com.projet.hirevisionai.Entity.SkillPriority;
import com.projet.hirevisionai.Entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class LearningPlanRepositoryTest {

    @Autowired
    private LearningPlanRepository learningPlanRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User user;
    private MissedSkill missedSkill;
    private Interview interview;

    @BeforeEach
    void setUp() {
        user = User.builder().fullName("Adem").email("adem@test.com")
                .password("p").age(22).role(Role.CANDIDATE).build();
        entityManager.persistAndFlush(user);

        // Chaîne pour LearningPlan issu du job matching : MissedSkill -> MatchingResult -> CV -> User
        CV cv = CV.builder().filePath("cv.pdf").uploadDate(LocalDate.now()).user(user).build();
        entityManager.persistAndFlush(cv);

        JobOffer jobOffer = JobOffer.builder().title("Backend Dev").active(true).build();
        entityManager.persistAndFlush(jobOffer);

        MatchingResult matchingResult = MatchingResult.builder().score(60f).cv(cv).jobOffer(jobOffer).build();
        entityManager.persistAndFlush(matchingResult);

        missedSkill = MissedSkill.builder()
                .skillName("Docker").priority(SkillPriority.HAUTE).matchingResult(matchingResult).build();
        entityManager.persistAndFlush(missedSkill);

        // Chaîne pour LearningPlan issu d'un entretien : Interview -> User
        interview = Interview.builder().startDate(LocalDateTime.now()).durationMinutes(30).user(user).build();
        entityManager.persistAndFlush(interview);
    }

    @Test
    void findByMissedSkillId_shouldReturnPlansForGivenMissedSkill() {
        entityManager.persistAndFlush(LearningPlan.builder()
                .title("Apprendre Docker").source(PlanSource.JOB_MATCHING).missedSkill(missedSkill).build());

        List<LearningPlan> result = learningPlanRepository.findByMissedSkillId(missedSkill.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Apprendre Docker");
    }

    @Test
    void findByMissedSkillMatchingResultCvUserIdUser_shouldReturnPlansForUser() {
        entityManager.persistAndFlush(LearningPlan.builder()
                .title("Apprendre Docker").source(PlanSource.JOB_MATCHING).missedSkill(missedSkill).build());

        List<LearningPlan> result = learningPlanRepository.findByMissedSkillMatchingResultCvUserIdUser(user.getIdUser());

        assertThat(result).hasSize(1);
    }

    @Test
    void findByTitleContainingIgnoreCase_shouldMatchPartialTitle() {
        entityManager.persistAndFlush(LearningPlan.builder()
                .title("Apprendre Docker").source(PlanSource.JOB_MATCHING).missedSkill(missedSkill).build());
        entityManager.persistAndFlush(LearningPlan.builder()
                .title("Maîtriser Kubernetes").source(PlanSource.JOB_MATCHING).missedSkill(missedSkill).build());

        List<LearningPlan> result = learningPlanRepository.findByTitleContainingIgnoreCase("docker");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Apprendre Docker");
    }

    @Test
    void findByInterviewId_shouldReturnPlansLinkedToInterview() {
        entityManager.persistAndFlush(LearningPlan.builder()
                .title("Revoir les bases SQL").source(PlanSource.INTERVIEW).interview(interview).build());

        List<LearningPlan> result = learningPlanRepository.findByInterviewId(interview.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Revoir les bases SQL");
    }

    @Test
    void findByInterviewUserIdUser_shouldReturnPlansForUserInterviews() {
        entityManager.persistAndFlush(LearningPlan.builder()
                .title("Revoir les bases SQL").source(PlanSource.INTERVIEW).interview(interview).build());

        List<LearningPlan> result = learningPlanRepository.findByInterviewUserIdUser(user.getIdUser());

        assertThat(result).hasSize(1);
    }

    @Test
    void deleteByInterviewId_shouldRemoveOnlyPlansOfThatInterview() {
        entityManager.persistAndFlush(LearningPlan.builder()
                .title("Plan Interview").source(PlanSource.INTERVIEW).interview(interview).build());
        entityManager.persistAndFlush(LearningPlan.builder()
                .title("Plan JobMatching").source(PlanSource.JOB_MATCHING).missedSkill(missedSkill).build());

        learningPlanRepository.deleteByInterviewId(interview.getId());
        entityManager.flush();
        entityManager.clear();

        List<LearningPlan> remaining = learningPlanRepository.findAll();
        assertThat(remaining).hasSize(1);
        assertThat(remaining.get(0).getTitle()).isEqualTo("Plan JobMatching");
    }
}
