package com.projet.hirevisionai.Repository;

import com.projet.hirevisionai.Entity.Plan;
import com.projet.hirevisionai.Entity.PlanType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PlanRepository ne définit aucune méthode custom : il hérite uniquement de
 * JpaRepository<Plan, PlanType>. On teste donc les opérations CRUD de base
 * fournies par Spring Data (findById, findAll, save...), pour s'assurer que
 * le mapping de l'entité (clé primaire = enum PlanType, @ElementCollection
 * "features") fonctionne correctement.
 */
@DataJpaTest
class PlanRepositoryTest {

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findById_shouldReturnPlan_whenKeyExists() {
        Plan plan = Plan.builder()
                .key(PlanType.PRO)
                .name("Pro")
                .price(19.99)
                .tagline("Pour les pros")
                .features(List.of("Feature 1", "Feature 2"))
                .build();
        entityManager.persistAndFlush(plan);

        Optional<Plan> result = planRepository.findById(PlanType.PRO);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Pro");
        assertThat(result.get().getFeatures()).containsExactly("Feature 1", "Feature 2");
    }

    @Test
    void findById_shouldReturnEmpty_whenKeyDoesNotExist() {
        Optional<Plan> result = planRepository.findById(PlanType.PREMIUM);

        assertThat(result).isEmpty();
    }

    @Test
    void findAll_shouldReturnAllPersistedPlans() {
        entityManager.persistAndFlush(Plan.builder().key(PlanType.PRO).name("Pro").price(19.99).build());
        entityManager.persistAndFlush(Plan.builder().key(PlanType.PREMIUM).name("Premium").price(39.99).build());

        List<Plan> result = planRepository.findAll();

        assertThat(result).hasSize(2)
                .extracting(Plan::getKey)
                .containsExactlyInAnyOrder(PlanType.PRO, PlanType.PREMIUM);
    }
}
