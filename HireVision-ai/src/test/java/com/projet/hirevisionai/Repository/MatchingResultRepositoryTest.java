package com.projet.hirevisionai.Repository;

import com.projet.hirevisionai.Entity.CV;
import com.projet.hirevisionai.Entity.JobOffer;
import com.projet.hirevisionai.Entity.MatchingResult;
import com.projet.hirevisionai.Entity.Role;
import com.projet.hirevisionai.Entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class MatchingResultRepositoryTest {

    @Autowired
    private MatchingResultRepository matchingResultRepository;

    @Autowired
    private TestEntityManager entityManager;

    private CV cv;
    private JobOffer jobOffer;

    @BeforeEach
    void setUp() {
        User user = User.builder().fullName("Adem").email("adem@test.com")
                .password("p").age(22).role(Role.CANDIDATE).build();
        entityManager.persistAndFlush(user);

        cv = CV.builder().filePath("cv.pdf").uploadDate(LocalDate.now()).user(user).build();
        entityManager.persistAndFlush(cv);

        jobOffer = JobOffer.builder().title("Backend Dev").active(true).build();
        entityManager.persistAndFlush(jobOffer);
    }

    @Test
    void findByCvId_shouldReturnAllMatchesForCv() {
        entityManager.persistAndFlush(MatchingResult.builder().score(75f).cv(cv).jobOffer(jobOffer).build());
        entityManager.persistAndFlush(MatchingResult.builder().score(60f).cv(cv).jobOffer(jobOffer).build());

        List<MatchingResult> result = matchingResultRepository.findByCvId(cv.getId());

        assertThat(result).hasSize(2);
    }

    @Test
    void findByCvUserIdUser_shouldReturnMatchesForGivenUser() {
        entityManager.persistAndFlush(MatchingResult.builder().score(80f).cv(cv).jobOffer(jobOffer).build());

        List<MatchingResult> result = matchingResultRepository.findByCvUserIdUser(cv.getUser().getIdUser());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getScore()).isEqualTo(80f);
    }

    @Test
    void findTopByCvIdOrderByScoreDesc_shouldReturnHighestScore() {
        entityManager.persistAndFlush(MatchingResult.builder().score(40f).cv(cv).jobOffer(jobOffer).build());
        entityManager.persistAndFlush(MatchingResult.builder().score(90f).cv(cv).jobOffer(jobOffer).build());
        entityManager.persistAndFlush(MatchingResult.builder().score(65f).cv(cv).jobOffer(jobOffer).build());

        Optional<MatchingResult> result = matchingResultRepository.findTopByCvIdOrderByScoreDesc(cv.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getScore()).isEqualTo(90f);
    }

    @Test
    void findByCvIdAndScoreGreaterThanEqual_shouldFilterByMinimumScore() {
        entityManager.persistAndFlush(MatchingResult.builder().score(40f).cv(cv).jobOffer(jobOffer).build());
        entityManager.persistAndFlush(MatchingResult.builder().score(75f).cv(cv).jobOffer(jobOffer).build());
        entityManager.persistAndFlush(MatchingResult.builder().score(90f).cv(cv).jobOffer(jobOffer).build());

        List<MatchingResult> result = matchingResultRepository.findByCvIdAndScoreGreaterThanEqual(cv.getId(), 75f);

        assertThat(result).hasSize(2)
                .extracting(MatchingResult::getScore)
                .containsExactlyInAnyOrder(75f, 90f);
    }
}
