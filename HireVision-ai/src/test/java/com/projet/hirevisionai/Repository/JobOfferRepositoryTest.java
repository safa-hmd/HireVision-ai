package com.projet.hirevisionai.Repository;

import com.projet.hirevisionai.Entity.JobOffer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class JobOfferRepositoryTest {

    @Autowired
    private JobOfferRepository jobOfferRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findByActiveTrue_shouldReturnOnlyActiveOffers() {
        entityManager.persistAndFlush(JobOffer.builder().title("Backend Dev").active(true).build());
        entityManager.persistAndFlush(JobOffer.builder().title("Old Offer").active(false).build());

        List<JobOffer> result = jobOfferRepository.findByActiveTrue();

        assertThat(result).extracting(JobOffer::getTitle).containsExactly("Backend Dev");
    }

    @Test
    void findByActiveTrue_shouldReturnEmptyList_whenNoActiveOffers() {
        entityManager.persistAndFlush(JobOffer.builder().title("Old Offer").active(false).build());

        List<JobOffer> result = jobOfferRepository.findByActiveTrue();

        assertThat(result).isEmpty();
    }

    @Test
    void findByTitleContainingIgnoreCase_shouldMatchRegardlessOfCase() {
        entityManager.persistAndFlush(JobOffer.builder().title("Java Backend Developer").active(true).build());
        entityManager.persistAndFlush(JobOffer.builder().title("Frontend Developer").active(true).build());

        List<JobOffer> result = jobOfferRepository.findByTitleContainingIgnoreCase("backend");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Java Backend Developer");
    }

    @Test
    void findByTitleContainingIgnoreCase_shouldReturnEmptyList_whenNoMatch() {
        entityManager.persistAndFlush(JobOffer.builder().title("Frontend Developer").active(true).build());

        List<JobOffer> result = jobOfferRepository.findByTitleContainingIgnoreCase("devops");

        assertThat(result).isEmpty();
    }
}
