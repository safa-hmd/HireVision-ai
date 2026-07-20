package com.projet.hirevisionai.Repository;

import com.projet.hirevisionai.Entity.Payment;
import com.projet.hirevisionai.Entity.PaymentStatus;
import com.projet.hirevisionai.Entity.PlanType;
import com.projet.hirevisionai.Entity.Role;
import com.projet.hirevisionai.Entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DataJpaTest
class PaymentRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().fullName("Adem").email("adem@test.com")
                .password("p").age(22).role(Role.CANDIDATE).build();
        entityManager.persistAndFlush(user);
    }

    private Payment newPayment(double amount, PaymentStatus status, LocalDateTime date) {
        return Payment.builder().user(user).plan(PlanType.PRO)
                .amount(amount).status(status).paymentDate(date).build();
    }

    @Test
    void findByUserIdUser_shouldReturnAllPaymentsOfUser() {
        entityManager.persistAndFlush(newPayment(19.99, PaymentStatus.PAID, LocalDateTime.now()));

        List<Payment> result = paymentRepository.findByUserIdUser(user.getIdUser());

        assertThat(result).hasSize(1);
    }

    @Test
    void findTop10ByOrderByPaymentDateDesc_shouldLimitAndSortDescending() {
        for (int i = 0; i < 12; i++) {
            entityManager.persistAndFlush(
                    newPayment(10 + i, PaymentStatus.PAID, LocalDateTime.now().minusDays(i)));
        }

        List<Payment> result = paymentRepository.findTop10ByOrderByPaymentDateDesc();

        assertThat(result).hasSize(10);
        // Le paiement le plus récent (i=0, minusDays(0)) doit être en tête.
        assertThat(result.get(0).getAmount()).isEqualTo(10.0);
    }

    @Test
    void sumPaidAmountBetween_shouldSumOnlyPaidPaymentsInRange() {
        LocalDateTime now = LocalDateTime.now();
        entityManager.persistAndFlush(newPayment(20.0, PaymentStatus.PAID, now));
        entityManager.persistAndFlush(newPayment(30.0, PaymentStatus.PAID, now));
        entityManager.persistAndFlush(newPayment(50.0, PaymentStatus.FAILED, now)); // ignoré
        entityManager.persistAndFlush(newPayment(99.0, PaymentStatus.PAID, now.minusDays(30))); // hors plage

        Double sum = paymentRepository.sumPaidAmountBetween(now.minusDays(1), now.plusDays(1));

        assertThat(sum).isCloseTo(50.0, within(0.01));
    }

    @Test
    void sumPaidAmountBetween_shouldReturnZero_whenNoPaymentsMatch() {
        LocalDateTime now = LocalDateTime.now();

        Double sum = paymentRepository.sumPaidAmountBetween(now.minusDays(1), now.plusDays(1));

        // COALESCE(SUM(...), 0) dans la requête garantit un 0 plutôt qu'un null.
        assertThat(sum).isZero();
    }

    @Test
    void countByStatusAndPaymentDateBetween_shouldCountMatchingPayments() {
        LocalDateTime now = LocalDateTime.now();
        entityManager.persistAndFlush(newPayment(20.0, PaymentStatus.PAID, now));
        entityManager.persistAndFlush(newPayment(30.0, PaymentStatus.FAILED, now));

        long count = paymentRepository.countByStatusAndPaymentDateBetween(
                PaymentStatus.PAID, now.minusDays(1), now.plusDays(1));

        assertThat(count).isEqualTo(1);
    }
}
