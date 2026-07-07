package com.projet.hirevisionai.ServiceImpl;

import com.projet.hirevisionai.Dto.*;
import com.projet.hirevisionai.Entity.*;
import com.projet.hirevisionai.Repository.PaymentRepository;
import com.projet.hirevisionai.Repository.SubscriptionRepository;
import com.projet.hirevisionai.Repository.UserRepository;
import com.projet.hirevisionai.ServiceInterface.ISubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements ISubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    private double monthlyPrice(PlanType plan) {
        switch (plan) {
            case PRO: return 29.0;
            case PREMIUM: return 59.0;
            default: return 0.0;
        }
    }

    @Override
    public SubscriptionStatsDTO getStatsOverview() {
        LocalDateTime now = LocalDateTime.now();

        long totalUsers = userRepository.count();
        long countPro = subscriptionRepository.countByStatusAndPlan(SubscriptionStatus.ACTIVE, PlanType.PRO);
        long countPremium = subscriptionRepository.countByStatusAndPlan(SubscriptionStatus.ACTIVE, PlanType.PREMIUM);
        long payingSubscribersCount = countPro + countPremium;
        long freeUsersCount = Math.max(totalUsers - payingSubscribersCount, 0);

        double mrr = countPro * monthlyPrice(PlanType.PRO) + countPremium * monthlyPrice(PlanType.PREMIUM);
        double arr = mrr * 12;
        double arpu = payingSubscribersCount > 0 ? mrr / payingSubscribersCount : 0.0;

        LocalDateTime startOfThisMonth = now.withDayOfMonth(1).toLocalDate().atStartOfDay();
        LocalDateTime startOfLastMonth = startOfThisMonth.minusMonths(1);
        double revenueThisMonth = safeSum(paymentRepository.sumPaidAmountBetween(startOfThisMonth, now));
        double revenueLastMonth = safeSum(paymentRepository.sumPaidAmountBetween(startOfLastMonth, startOfThisMonth));
        double mrrChangePercent = revenueLastMonth > 0
                ? ((revenueThisMonth - revenueLastMonth) * 100.0) / revenueLastMonth
                : (revenueThisMonth > 0 ? 100.0 : 0.0);

        List<CategoryCountDTO> planDistribution = List.of(
                CategoryCountDTO.builder().label("Gratuit").value(freeUsersCount).build(),
                CategoryCountDTO.builder().label("Pro").value(countPro).build(),
                CategoryCountDTO.builder().label("Premium").value(countPremium).build()
        );

        List<DayCountDTO> revenueLast6Months = buildLast6MonthsRevenue(now);

        List<PaymentDTO> recentTransactions = paymentRepository.findTop10ByOrderByPaymentDateDesc()
                .stream()
                .map(PaymentDTO::fromEntity)
                .collect(Collectors.toList());

        return SubscriptionStatsDTO.builder()
                .mrr(round1(mrr))
                .mrrChangePercent(round1(mrrChangePercent))
                .arr(round1(arr))
                .payingSubscribersCount(payingSubscribersCount)
                .totalUsers(totalUsers)
                .arpu(round1(arpu))
                .planDistribution(planDistribution)
                .revenueLast6Months(revenueLast6Months)
                .recentTransactions(recentTransactions)
                .build();
    }

    private List<DayCountDTO> buildLast6MonthsRevenue(LocalDateTime now) {
        List<DayCountDTO> result = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDateTime monthStart = now.withDayOfMonth(1).toLocalDate().atStartOfDay().minusMonths(i);
            LocalDateTime monthEnd = monthStart.plusMonths(1);
            double sum = safeSum(paymentRepository.sumPaidAmountBetween(monthStart, monthEnd));
            String label = monthStart.getMonth().getDisplayName(TextStyle.SHORT, Locale.FRENCH);
            label = label.substring(0, 1).toUpperCase() + label.substring(1);
            result.add(DayCountDTO.builder().label(label).value(Math.round(sum)).build());
        }
        return result;
    }

    @Override
    public SubscriptionDTO subscribe(SubscriptionCreateRequestDTO request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable : " + request.getUserId()));

        // Annule l'abonnement actif existant s'il y en a un
        subscriptionRepository.findByUserIdUserAndStatus(user.getIdUser(), SubscriptionStatus.ACTIVE)
                .ifPresent(existing -> {
                    existing.setStatus(SubscriptionStatus.CANCELED);
                    subscriptionRepository.save(existing);
                });

        LocalDateTime now = LocalDateTime.now();
        Subscription subscription = Subscription.builder()
                .user(user)
                .plan(request.getPlan())
                .status(SubscriptionStatus.ACTIVE)
                .startDate(now)
                .renewalDate(now.plusMonths(1))
                .build();
        subscription = subscriptionRepository.save(subscription);

        Payment payment = Payment.builder()
                .user(user)
                .subscription(subscription)
                .plan(request.getPlan())
                .amount(monthlyPrice(request.getPlan()))
                .status(PaymentStatus.PAID)
                .paymentDate(now)
                .build();
        paymentRepository.save(payment);

        return SubscriptionDTO.fromEntity(subscription);
    }

    @Override
    public SubscriptionDTO cancel(Long userId) {
        Subscription subscription = subscriptionRepository
                .findByUserIdUserAndStatus(userId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("Aucun abonnement actif pour cet utilisateur"));
        subscription.setStatus(SubscriptionStatus.CANCELED);
        subscription = subscriptionRepository.save(subscription);
        return SubscriptionDTO.fromEntity(subscription);
    }

    @Override
    public List<SubscriptionDTO> getByUserId(Long userId) {
        return subscriptionRepository.findByUserIdUser(userId)
                .stream()
                .map(SubscriptionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<PaymentDTO> getPaymentsByUserId(Long userId) {
        return paymentRepository.findByUserIdUser(userId)
                .stream()
                .map(PaymentDTO::fromEntity)
                .collect(Collectors.toList());
    }

    private double safeSum(Double value) {
        return value != null ? value : 0.0;
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
