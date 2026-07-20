package com.projet.hirevisionai.Repository;

import com.projet.hirevisionai.Entity.CV;
import com.projet.hirevisionai.Entity.Feedback;
import com.projet.hirevisionai.Entity.Interview;
import com.projet.hirevisionai.Entity.Role;
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
import static org.assertj.core.api.Assertions.within;

@DataJpaTest
class InterviewRepositoryTest {

    @Autowired
    private InterviewRepository interviewRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User user;
    private CV cv;

    @BeforeEach
    void setUp() {
        user = User.builder().fullName("Adem").email("adem@test.com")
                .password("p").age(22).role(Role.CANDIDATE).build();
        entityManager.persistAndFlush(user);

        cv = CV.builder().filePath("cv.pdf").uploadDate(LocalDate.now()).user(user).build();
        entityManager.persistAndFlush(cv);
    }

    private Interview newInterview(LocalDateTime start) {
        return Interview.builder().startDate(start).durationMinutes(30).user(user).cv(cv).build();
    }

    @Test
    void findByUserIdUser_shouldReturnInterviewsOfUser() {
        entityManager.persistAndFlush(newInterview(LocalDateTime.now()));

        List<Interview> result = interviewRepository.findByUserIdUser(user.getIdUser());

        assertThat(result).hasSize(1);
    }

    @Test
    void findByCvId_shouldReturnInterviewsLinkedToCv() {
        entityManager.persistAndFlush(newInterview(LocalDateTime.now()));

        List<Interview> result = interviewRepository.findByCvId(cv.getId());

        assertThat(result).hasSize(1);
    }

    @Test
    void findByUserIdUserOrderByStartDateDesc_shouldReturnMostRecentFirst() {
        entityManager.persistAndFlush(newInterview(LocalDateTime.now().minusDays(3)));
        entityManager.persistAndFlush(newInterview(LocalDateTime.now()));

        List<Interview> result = interviewRepository.findByUserIdUserOrderByStartDateDesc(user.getIdUser());

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getStartDate()).isAfter(result.get(1).getStartDate());
    }

    @Test
    void countByUserIdUser_shouldReturnCorrectCount() {
        entityManager.persistAndFlush(newInterview(LocalDateTime.now()));
        entityManager.persistAndFlush(newInterview(LocalDateTime.now()));

        long count = interviewRepository.countByUserIdUser(user.getIdUser());

        assertThat(count).isEqualTo(2);
    }

    @Test
    void findAverageTechnicalScoreByUserId_shouldComputeAverage() {
        Interview i1 = newInterview(LocalDateTime.now());
        entityManager.persistAndFlush(i1);
        Feedback f1 = Feedback.builder().technicalScore(80f).communicationScore(70f)
                .confidenceScore(60f).eyeContactScore(50f).interview(i1).build();
        entityManager.persistAndFlush(f1);

        Interview i2 = newInterview(LocalDateTime.now());
        entityManager.persistAndFlush(i2);
        Feedback f2 = Feedback.builder().technicalScore(60f).communicationScore(70f)
                .confidenceScore(60f).eyeContactScore(50f).interview(i2).build();
        entityManager.persistAndFlush(f2);

        Double avg = interviewRepository.findAverageTechnicalScoreByUserId(user.getIdUser());

        assertThat(avg).isEqualTo(70.0);
    }

    @Test
    void findByUserIdAndDateRange_shouldReturnOnlyInterviewsWithinRange() {
        LocalDateTime now = LocalDateTime.now();
        entityManager.persistAndFlush(newInterview(now.minusDays(10))); // hors plage
        entityManager.persistAndFlush(newInterview(now));               // dans la plage

        List<Interview> result = interviewRepository.findByUserIdAndDateRange(
                user.getIdUser(), now.minusDays(1), now.plusDays(1));

        assertThat(result).hasSize(1);
    }

    @Test
    void countByStartDateBetween_shouldCountInterviewsInRange() {
        LocalDateTime now = LocalDateTime.now();
        entityManager.persistAndFlush(newInterview(now));

        long count = interviewRepository.countByStartDateBetween(now.minusDays(1), now.plusDays(1));

        assertThat(count).isEqualTo(1);
    }

    @Test
    void findTop5ByOrderByStartDateDesc_shouldLimitToFiveMostRecent() {
        for (int i = 0; i < 7; i++) {
            entityManager.persistAndFlush(newInterview(LocalDateTime.now().minusDays(i)));
        }

        List<Interview> result = interviewRepository.findTop5ByOrderByStartDateDesc();

        assertThat(result).hasSize(5);
    }

    @Test
    void findAllByOrderByStartDateDesc_shouldReturnAllSorted() {
        entityManager.persistAndFlush(newInterview(LocalDateTime.now().minusDays(2)));
        entityManager.persistAndFlush(newInterview(LocalDateTime.now()));

        List<Interview> result = interviewRepository.findAllByOrderByStartDateDesc();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getStartDate()).isAfter(result.get(1).getStartDate());
    }

    @Test
    void findAvgScoreByDateRange_shouldAverageAllFourFeedbackScores() {
        LocalDateTime now = LocalDateTime.now();
        Interview interview = newInterview(now);
        entityManager.persistAndFlush(interview);
        // moyenne de (80+80+80+80)/4 = 80
        entityManager.persistAndFlush(Feedback.builder()
                .technicalScore(80f).communicationScore(80f)
                .confidenceScore(80f).eyeContactScore(80f).interview(interview).build());

        Double avg = interviewRepository.findAvgScoreByDateRange(now.minusDays(1), now.plusDays(1));

        assertThat(avg).isCloseTo(80.0, within(0.01));
    }
}
