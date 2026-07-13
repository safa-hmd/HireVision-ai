package com.projet.hirevisionai.Repository;

import com.projet.hirevisionai.Entity.MissedSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MissedSkillRepository extends JpaRepository<MissedSkill, Long> {
    List<MissedSkill> findByMatchingResultId(Long matchingResultId);
    List<MissedSkill> findBySkillNameIgnoreCase(String skillName);
    long countByMatchingResultId(Long matchingResultId);

    // Toutes les compétences manquantes de l'user, tous matchings confondus
    List<MissedSkill> findByMatchingResultCvUserIdUser(Long userId);
}