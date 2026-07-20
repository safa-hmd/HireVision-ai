package com.projet.hirevisionai.Repository;

import com.projet.hirevisionai.Entity.Feedback;
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
class FeedbackRepositoryTest {

    @Autowired
    private FeedbackRepository feedbackRepository;

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
    void findByInterviewId_shouldReturnFeedback_whenExists() {
        entityManager.persistAndFlush(Feedback.builder()
                .technicalScore(80f).communicationScore(70f)
                .confidenceScore(60f).eyeContactScore(50f).interview(interview).build());

        Optional<Feedback> result = feedbackRepository.findByInterviewId(interview.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getTechnicalScore()).isEqualTo(80f);
    }

    @Test
    void existsByInterviewId_shouldReturnTrue_whenFeedbackExists() {
        entityManager.persistAndFlush(Feedback.builder()
                .technicalScore(80f).communicationScore(70f)
                .confidenceScore(60f).eyeContactScore(50f).interview(interview).build());

        assertThat(feedbackRepository.existsByInterviewId(interview.getId())).isTrue();
    }

    @Test
    void existsByInterviewId_shouldReturnFalse_whenNoFeedback() {
        assertThat(feedbackRepository.existsByInterviewId(interview.getId())).isFalse();
    }

    @Test
    void findByInterviewUserIdUser_shouldReturnAllFeedbacksOfUser() {
        entityManager.persistAndFlush(Feedback.builder()
                .technicalScore(80f).communicationScore(70f)
                .confidenceScore(60f).eyeContactScore(50f).interview(interview).build());

        List<Feedback> result = feedbackRepository.findByInterviewUserIdUser(user.getIdUser());

        assertThat(result).hasSize(1);
    }

    @Test
    void findAvgTechnicalScoreByUserId_shouldAverageTechnicalScores() {
        entityManager.persistAndFlush(Feedback.builder()
                .technicalScore(60f).communicationScore(70f)
                .confidenceScore(60f).eyeContactScore(50f).interview(interview).build());

        Double avg = feedbackRepository.findAvgTechnicalScoreByUserId(user.getIdUser());

        assertThat(avg).isCloseTo(60.0, within(0.01));
    }

    @Test
    void findAvgCommunicationScoreByUserId_shouldAverageCommunicationScores() {
        entityManager.persistAndFlush(Feedback.builder()
                .technicalScore(60f).communicationScore(90f)
                .confidenceScore(60f).eyeContactScore(50f).interview(interview).build());

        Double avg = feedbackRepository.findAvgCommunicationScoreByUserId(user.getIdUser());

        assertThat(avg).isCloseTo(90.0, within(0.01));
    }

    @Test
    void findAvgConfidenceScoreByUserId_shouldAverageConfidenceScores() {
        entityManager.persistAndFlush(Feedback.builder()
                .technicalScore(60f).communicationScore(70f)
                .confidenceScore(100f).eyeContactScore(50f).interview(interview).build());

        Double avg = feedbackRepository.findAvgConfidenceScoreByUserId(user.getIdUser());

        assertThat(avg).isCloseTo(100.0, within(0.01));
    }

    @Test
    void findGlobalAverageScore_shouldAverageAllFourScoresAcrossAllFeedbacks() {
        entityManager.persistAndFlush(Feedback.builder()
                .technicalScore(80f).communicationScore(80f)
                .confidenceScore(80f).eyeContactScore(80f).interview(interview).build());

        Double avg = feedbackRepository.findGlobalAverageScore();

        assertThat(avg).isCloseTo(80.0, within(0.01));
    }
}
