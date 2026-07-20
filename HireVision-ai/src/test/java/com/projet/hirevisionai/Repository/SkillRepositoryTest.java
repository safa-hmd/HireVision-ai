package com.projet.hirevisionai.Repository;

import com.projet.hirevisionai.Entity.Skill;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class SkillRepositoryTest {

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findByName_shouldReturnSkill_whenNameExists() {
        entityManager.persistAndFlush(Skill.builder().name("Java").category("Langage").build());

        Optional<Skill> result = skillRepository.findByName("Java");

        assertThat(result).isPresent();
        assertThat(result.get().getCategory()).isEqualTo("Langage");
    }

    @Test
    void findByName_shouldReturnEmpty_whenNameDoesNotExist() {
        Optional<Skill> result = skillRepository.findByName("Inconnu");

        assertThat(result).isEmpty();
    }

    @Test
    void existsByName_shouldReturnTrue_whenSkillExists() {
        entityManager.persistAndFlush(Skill.builder().name("Docker").category("DevOps").build());

        boolean exists = skillRepository.existsByName("Docker");

        assertThat(exists).isTrue();
    }

    @Test
    void existsByName_shouldReturnFalse_whenSkillDoesNotExist() {
        boolean exists = skillRepository.existsByName("Inconnu");

        assertThat(exists).isFalse();
    }

    @Test
    void findByCategory_shouldReturnMatchingSkills() {
        entityManager.persistAndFlush(Skill.builder().name("Java").category("Langage").build());
        entityManager.persistAndFlush(Skill.builder().name("Python").category("Langage").build());
        entityManager.persistAndFlush(Skill.builder().name("Docker").category("DevOps").build());

        List<Skill> result = skillRepository.findByCategory("Langage");

        assertThat(result).extracting(Skill::getName).containsExactlyInAnyOrder("Java", "Python");
    }

    @Test
    void findByNameContainingIgnoreCase_shouldMatchPartialAndCaseInsensitive() {
        entityManager.persistAndFlush(Skill.builder().name("JavaScript").category("Langage").build());
        entityManager.persistAndFlush(Skill.builder().name("Java").category("Langage").build());

        List<Skill> result = skillRepository.findByNameContainingIgnoreCase("java");

        assertThat(result).extracting(Skill::getName).containsExactlyInAnyOrder("JavaScript", "Java");
    }
}
