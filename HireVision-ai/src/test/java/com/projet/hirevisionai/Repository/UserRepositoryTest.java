package com.projet.hirevisionai.Repository;

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

/**
 * Test de la couche Repository (accès aux données) pour UserRepository.
 *
 * @DataJpaTest : ne charge que la couche JPA (EntityManager, Repositories,
 * base H2 en mémoire), pas les contrôleurs ni les services. Chaque test est
 * exécuté dans une transaction qui est annulée (rollback) à la fin, donc
 * les tests ne se polluent pas entre eux et n'ont pas besoin de nettoyer
 * la base manuellement.
 */
@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    // TestEntityManager permet de préparer des données de test directement
    // en base (comme le ferait un vrai flush SQL), indépendamment
    // du repository que l'on est en train de tester.
    @Autowired
    private TestEntityManager entityManager;

    private User existingUser;

    @BeforeEach
    void setUp() {
        existingUser = User.builder()
                .fullName("Adem Ben")
                .email("adem@test.com")
                .password("encoded-pass")
                .age(22)
                .role(Role.CANDIDATE)
                .enabled(true)
                .build();

        entityManager.persistAndFlush(existingUser);
    }

    @Test
    void findByEmail_shouldReturnUser_whenEmailExists() {
        Optional<User> result = userRepository.findByEmail("adem@test.com");

        assertThat(result).isPresent();
        assertThat(result.get().getFullName()).isEqualTo("Adem Ben");
    }

    @Test
    void findByEmail_shouldReturnEmpty_whenEmailDoesNotExist() {
        Optional<User> result = userRepository.findByEmail("inconnu@test.com");

        assertThat(result).isEmpty();
    }

    @Test
    void findByResetToken_shouldReturnUser_whenTokenMatches() {
        existingUser.setResetToken("reset-token-123");
        entityManager.persistAndFlush(existingUser);

        Optional<User> result = userRepository.findByResetToken("reset-token-123");

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("adem@test.com");
    }

    @Test
    void findByResetToken_shouldReturnEmpty_whenTokenDoesNotMatch() {
        Optional<User> result = userRepository.findByResetToken("token-inexistant");

        assertThat(result).isEmpty();
    }

    @Test
    void findTop5ByOrderByCreatedAtDesc_shouldReturnMostRecentUsersFirst() {
        // On crée 3 utilisateurs supplémentaires. createdAt est rempli
        // automatiquement par @CreationTimestamp au moment du persist.
        User user2 = User.builder().fullName("User 2").email("u2@test.com")
                .password("p").age(25).role(Role.CANDIDATE).build();
        User user3 = User.builder().fullName("User 3").email("u3@test.com")
                .password("p").age(30).role(Role.CANDIDATE).build();

        entityManager.persistAndFlush(user2);
        entityManager.persistAndFlush(user3);

        List<User> result = userRepository.findTop5ByOrderByCreatedAtDesc();

        assertThat(result).hasSize(3);
        // Le plus récent (persisté en dernier) doit être en tête de liste.
        assertThat(result.get(0).getEmail()).isEqualTo("u3@test.com");
    }

    @Test
    void countByCreatedAtAfter_shouldCountOnlyUsersCreatedAfterGivenDate() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(1);

        long count = userRepository.countByCreatedAtAfter(cutoff);

        // existingUser vient d'être créé (setUp), donc il est après le cutoff.
        assertThat(count).isEqualTo(1);
    }

    @Test
    void countByCreatedAtAfter_shouldReturnZero_whenCutoffIsInTheFuture() {
        LocalDateTime cutoff = LocalDateTime.now().plusDays(1);

        long count = userRepository.countByCreatedAtAfter(cutoff);

        assertThat(count).isZero();
    }

    @Test
    void findByCreatedAtAfter_shouldReturnMatchingUsers() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(1);

        List<User> result = userRepository.findByCreatedAtAfter(cutoff);

        assertThat(result).extracting(User::getEmail).contains("adem@test.com");
    }
}
