package com.projet.hirevisionai.Repository;

import com.projet.hirevisionai.Entity.BehaviorAnalysis;
import com.projet.hirevisionai.Entity.Interview;
import com.projet.hirevisionai.Entity.Role;
import com.projet.hirevisionai.Entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DataJpaTest
class BehaviorAnalysisRepositoryTest {

    @Autowired
    private BehaviorAnalysisRepository behaviorAnalysisRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User user;
    private Interview interview;

    @BeforeEach
    void setUp() {
        user = User.builder().fullName("Adem").email("adem@test.com")
                .password("p").age(22).role(Role.CANDIDATE).build();
        entityManager.persistAndFlush(user);

        interview = Interview.builder().startDate(LocalDateTime.now()).durationMinutes(30).user(user).build();
        entityManager.persistAndFlush(interview);
    }

    @Test
    void findByInterviewId_shouldReturnAnalysis_whenExists() {
        entityManager.persistAndFlush(BehaviorAnalysis.builder()
                .postureScore(70f).eyeContactScore(60f).expressionScore(80f)
                .videoPath("video.mp4").interview(interview).build());

        Optional<BehaviorAnalysis> result = behaviorAnalysisRepository.findByInterviewId(interview.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getVideoPath()).isEqualTo("video.mp4");
    }

    @Test
    void existsByInterviewId_shouldReturnFalse_whenNoAnalysis() {
        assertThat(behaviorAnalysisRepository.existsByInterviewId(interview.getId())).isFalse();
    }

    @Test
    void findByInterviewUserIdUser_shouldReturnAnalysesOfUser() {
        entityManager.persistAndFlush(BehaviorAnalysis.builder()
                .postureScore(70f).eyeContactScore(60f).expressionScore(80f).interview(interview).build());

        List<BehaviorAnalysis> result = behaviorAnalysisRepository.findByInterviewUserIdUser(user.getIdUser());

        assertThat(result).hasSize(1);
    }

    @Test
    void findAvgPostureScoreByUserId_shouldComputeAverage() {
        entityManager.persistAndFlush(BehaviorAnalysis.builder()
                .postureScore(90f).eyeContactScore(60f).expressionScore(80f).interview(interview).build());

        Double avg = behaviorAnalysisRepository.findAvgPostureScoreByUserId(user.getIdUser());

        assertThat(avg).isCloseTo(90.0, within(0.01));
    }

    @Test
    void findAvgEyeContactScoreByUserId_shouldComputeAverage() {
        entityManager.persistAndFlush(BehaviorAnalysis.builder()
                .postureScore(90f).eyeContactScore(55f).expressionScore(80f).interview(interview).build());

        Double avg = behaviorAnalysisRepository.findAvgEyeContactScoreByUserId(user.getIdUser());

        assertThat(avg).isCloseTo(55.0, within(0.01));
    }
}
