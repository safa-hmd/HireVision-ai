package com.projet.hirevisionai.Dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionStatsDTO {

    private double mrr; // revenus mensuels récurrents, basé sur les abonnements actifs
    private double mrrChangePercent;

    private double arr; // mrr * 12

    private long payingSubscribersCount;
    private long totalUsers;

    private double arpu; // mrr / payingSubscribersCount

    private List<CategoryCountDTO> planDistribution; // Gratuit / Pro / Premium
    private List<DayCountDTO> revenueLast6Months;     // label = mois, value = montant encaissé

    private List<PaymentDTO> recentTransactions;
}
