package com.projet.hirevisionai.Repository;

import com.projet.hirevisionai.Entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {
    Optional<Skill> findByName(String name);
    boolean existsByName(String name);
    List<Skill> findByCategory(String category);
    List<Skill> findByNameContainingIgnoreCase(String keyword);
}