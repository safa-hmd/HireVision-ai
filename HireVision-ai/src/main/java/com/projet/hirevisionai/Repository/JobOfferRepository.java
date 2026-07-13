package com.projet.hirevisionai.Repository;

import com.projet.hirevisionai.Entity.JobOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobOfferRepository extends JpaRepository<JobOffer, Long> {
    List<JobOffer> findByActiveTrue();
    List<JobOffer> findByTitleContainingIgnoreCase(String keyword);
}
