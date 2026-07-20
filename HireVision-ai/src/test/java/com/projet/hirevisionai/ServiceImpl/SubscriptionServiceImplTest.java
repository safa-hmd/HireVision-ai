package com.projet.hirevisionai.ServiceImpl;

import com.projet.hirevisionai.Dto.PaymentDTO;
import com.projet.hirevisionai.Dto.SubscriptionCreateRequestDTO;
import com.projet.hirevisionai.Dto.SubscriptionDTO;
import com.projet.hirevisionai.Dto.SubscriptionStatsDTO;
import com.projet.hirevisionai.Entity.*;
import com.projet.hirevisionai.Repository.PaymentRepository;
import com.projet.hirevisionai.Repository.SubscriptionRepository;
import com.projet.hirevisionai.Repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceImplTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SubscriptionServiceImpl subscriptionService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().idUser(1L).fullName("Jean Dupont").build();
    }

    @Test
    void subscribeTest_shouldCreateSubscriptionAndPayment_whenNoExistingSubscription() {
        SubscriptionCreateRequestDTO request = SubscriptionCreateRequestDTO.builder()
                .userId(1L).plan(PlanType.PRO).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUserIdUserAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        SubscriptionDTO result = subscriptionService.subscribe(request);

        assertThat(result.getPlan()).isEqualTo("PRO");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void subscribeTest_shouldCancelExistingSubscription_beforeCreatingNew() {
        SubscriptionCreateRequestDTO request = SubscriptionCreateRequestDTO.builder()
                .userId(1L).plan(PlanType.PREMIUM).build();
        Subscription existing = Subscription.builder().id(5L).user(user).plan(PlanType.PRO)
                .status(SubscriptionStatus.ACTIVE).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findByUserIdUserAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(existing));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        subscriptionService.subscribe(request);

        assertThat(existing.getStatus()).isEqualTo(SubscriptionStatus.CANCELED);
        verify(subscriptionRepository, times(2)).save(any(Subscription.class));
    }

    @Test
    void subscribeTest_shouldThrow_whenUserNotFound() {
        SubscriptionCreateRequestDTO request = SubscriptionCreateRequestDTO.builder()
                .userId(99L).plan(PlanType.PRO).build();
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.subscribe(request)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void cancelTest_shouldSetStatusCanceled_whenActiveSubscriptionExists() {
        Subscription subscription = Subscription.builder().id(1L).user(user).plan(PlanType.PRO)
                .status(SubscriptionStatus.ACTIVE).build();

        when(subscriptionRepository.findByUserIdUserAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(inv -> inv.getArgument(0));

        SubscriptionDTO result = subscriptionService.cancel(1L);

        assertThat(result.getStatus()).isEqualTo("CANCELED");
    }

    @Test
    void cancelTest_shouldThrow_whenNoActiveSubscription() {
        when(subscriptionRepository.findByUserIdUserAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.cancel(1L)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void getByUserIdTest_shouldReturnMappedList() {
        Subscription subscription = Subscription.builder().id(1L).user(user)
                .plan(PlanType.PRO).status(SubscriptionStatus.ACTIVE).build();
        when(subscriptionRepository.findByUserIdUser(1L)).thenReturn(List.of(subscription));

        assertThat(subscriptionService.getByUserId(1L)).hasSize(1);
    }

    @Test
    void getPaymentsByUserIdTest_shouldReturnMappedList() {
        Payment payment = Payment.builder().id(1L).user(user).plan(PlanType.PRO)
                .amount(29.0).status(PaymentStatus.PAID).build();
        when(paymentRepository.findByUserIdUser(1L)).thenReturn(List.of(payment));

        List<PaymentDTO> result = subscriptionService.getPaymentsByUserId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAmount()).isEqualTo(29.0);
    }

    @Test
    void getStatsOverviewTest_shouldComputeMrrAndArr_withNoData() {
        when(userRepository.count()).thenReturn(0L);
        when(subscriptionRepository.countByStatusAndPlan(SubscriptionStatus.ACTIVE, PlanType.PRO)).thenReturn(0L);
        when(subscriptionRepository.countByStatusAndPlan(SubscriptionStatus.ACTIVE, PlanType.PREMIUM)).thenReturn(0L);
        when(paymentRepository.sumPaidAmountBetween(any(), any())).thenReturn(0.0);
        when(paymentRepository.findTop10ByOrderByPaymentDateDesc()).thenReturn(List.of());

        SubscriptionStatsDTO result = subscriptionService.getStatsOverview();

        assertThat(result.getMrr()).isZero();
        assertThat(result.getArr()).isZero();
        assertThat(result.getTotalUsers()).isZero();
    }
}
