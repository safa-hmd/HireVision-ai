package com.projet.hirevisionai.Repository;

import com.projet.hirevisionai.Entity.PlanType;
import com.projet.hirevisionai.Entity.Role;
import com.projet.hirevisionai.Entity.Subscription;
import com.projet.hirevisionai.Entity.SubscriptionStatus;
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

@DataJpaTest
class SubscriptionRepositoryTest {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().fullName("Adem").email("adem@test.com")
                .password("p").age(22).role(Role.CANDIDATE).build();
        entityManager.persistAndFlush(user);
    }

    @Test
    void findByUserIdUser_shouldReturnAllSubscriptionsOfUser() {
        entityManager.persistAndFlush(Subscription.builder()
                .user(user).plan(PlanType.PRO).status(SubscriptionStatus.EXPIRED)
                .startDate(LocalDateTime.now().minusMonths(2)).build());
        entityManager.persistAndFlush(Subscription.builder()
                .user(user).plan(PlanType.PREMIUM).status(SubscriptionStatus.ACTIVE)
                .startDate(LocalDateTime.now()).build());

        List<Subscription> result = subscriptionRepository.findByUserIdUser(user.getIdUser());

        assertThat(result).hasSize(2);
    }

    @Test
    void findByUserIdUserAndStatus_shouldReturnOnlyMatchingStatus() {
        entityManager.persistAndFlush(Subscription.builder()
                .user(user).plan(PlanType.PRO).status(SubscriptionStatus.EXPIRED)
                .startDate(LocalDateTime.now().minusMonths(2)).build());
        entityManager.persistAndFlush(Subscription.builder()
                .user(user).plan(PlanType.PREMIUM).status(SubscriptionStatus.ACTIVE)
                .startDate(LocalDateTime.now()).build());

        Optional<Subscription> result = subscriptionRepository
                .findByUserIdUserAndStatus(user.getIdUser(), SubscriptionStatus.ACTIVE);

        assertThat(result).isPresent();
        assertThat(result.get().getPlan()).isEqualTo(PlanType.PREMIUM);
    }

    @Test
    void countByStatusAndPlan_shouldCountMatchingSubscriptions() {
        entityManager.persistAndFlush(Subscription.builder()
                .user(user).plan(PlanType.PRO).status(SubscriptionStatus.ACTIVE)
                .startDate(LocalDateTime.now()).build());
        entityManager.persistAndFlush(Subscription.builder()
                .user(user).plan(PlanType.PREMIUM).status(SubscriptionStatus.ACTIVE)
                .startDate(LocalDateTime.now()).build());

        long count = subscriptionRepository.countByStatusAndPlan(SubscriptionStatus.ACTIVE, PlanType.PRO);

        assertThat(count).isEqualTo(1);
    }

    @Test
    void countByStatus_shouldCountAllSubscriptionsWithGivenStatus() {
        entityManager.persistAndFlush(Subscription.builder()
                .user(user).plan(PlanType.PRO).status(SubscriptionStatus.CANCELED)
                .startDate(LocalDateTime.now()).build());

        long count = subscriptionRepository.countByStatus(SubscriptionStatus.CANCELED);

        assertThat(count).isEqualTo(1);
    }

    @Test
    void countActivePayingUsers_shouldCountOnlyActiveSubscriptions() {
        entityManager.persistAndFlush(Subscription.builder()
                .user(user).plan(PlanType.PRO).status(SubscriptionStatus.ACTIVE)
                .startDate(LocalDateTime.now()).build());
        entityManager.persistAndFlush(Subscription.builder()
                .user(user).plan(PlanType.PREMIUM).status(SubscriptionStatus.EXPIRED)
                .startDate(LocalDateTime.now()).build());

        long count = subscriptionRepository.countActivePayingUsers();

        assertThat(count).isEqualTo(1);
    }
}
