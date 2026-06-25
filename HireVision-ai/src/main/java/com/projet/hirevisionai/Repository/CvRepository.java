package com.projet.hirevisionai.Repository;

import com.projet.hirevisionai.Entity.CV;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CvRepository extends JpaRepository<CV, Long> {
    List<CV> findByUserIdUser(Long userId);
    Optional<CV> findTopByUserIdUserOrderByUploadDateDesc(Long userId);
    long countByUserIdUser(Long userId);

}
