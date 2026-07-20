package com.projet.hirevisionai.Repository;

import com.projet.hirevisionai.Entity.CV;
import com.projet.hirevisionai.Entity.JobOffer;
import com.projet.hirevisionai.Entity.MatchingResult;
import com.projet.hirevisionai.Entity.MissedSkill;
import com.projet.hirevisionai.Entity.Role;
import com.projet.hirevisionai.Entity.SkillPriority;
import com.projet.hirevisionai.Entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class MissedSkillRepositoryTest {

    @Autowired
    private MissedSkillRepository missedSkillRepository;

    @Autowired
    private TestEntityManager entityManager;

    private MatchingResult matchingResult;
    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().fullName("Adem").email("adem@test.com")
                .password("p").age(22).role(Role.CANDIDATE).build();
        entityManager.persistAndFlush(user);

        CV cv = CV.builder().filePath("cv.pdf").uploadDate(LocalDate.now()).user(user).build();
        entityManager.persistAndFlush(cv);

        JobOffer jobOffer = JobOffer.builder().title("Backend Dev").active(true).build();
        entityManager.persistAndFlush(jobOffer);

        matchingResult = MatchingResult.builder().score(60f).cv(cv).jobOffer(jobOffer).build();
        entityManager.persistAndFlush(matchingResult);
    }

    @Test
    void findByMatchingResultId_shouldReturnAssociatedMissedSkills() {
        entityManager.persistAndFlush(MissedSkill.builder()
                .skillName("Docker").priority(SkillPriority.HAUTE).matchingResult(matchingResult).build());
        entityManager.persistAndFlush(MissedSkill.builder()
                .skillName("Kubernetes").priority(SkillPriority.MOYENNE).matchingResult(matchingResult).build());

        List<MissedSkill> result = missedSkillRepository.findByMatchingResultId(matchingResult.getId());

        assertThat(result).hasSize(2)
                .extracting(MissedSkill::getSkillName)
                .containsExactlyInAnyOrder("Docker", "Kubernetes");
    }

    @Test
    void findBySkillNameIgnoreCase_shouldMatchRegardlessOfCase() {
        entityManager.persistAndFlush(MissedSkill.builder()
                .skillName("Docker").priority(SkillPriority.HAUTE).matchingResult(matchingResult).build());

        List<MissedSkill> result = missedSkillRepository.findBySkillNameIgnoreCase("docker");

        assertThat(result).hasSize(1);
    }

    @Test
    void countByMatchingResultId_shouldReturnCorrectCount() {
        entityManager.persistAndFlush(MissedSkill.builder()
                .skillName("Docker").priority(SkillPriority.HAUTE).matchingResult(matchingResult).build());
        entityManager.persistAndFlush(MissedSkill.builder()
                .skillName("AWS").priority(SkillPriority.BASSE).matchingResult(matchingResult).build());

        long count = missedSkillRepository.countByMatchingResultId(matchingResult.getId());

        assertThat(count).isEqualTo(2);
    }

    @Test
    void findByMatchingResultCvUserIdUser_shouldReturnAllMissedSkillsOfUser() {
        entityManager.persistAndFlush(MissedSkill.builder()
                .skillName("Docker").priority(SkillPriority.HAUTE).matchingResult(matchingResult).build());

        List<MissedSkill> result = missedSkillRepository.findByMatchingResultCvUserIdUser(user.getIdUser());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSkillName()).isEqualTo("Docker");
    }
}
