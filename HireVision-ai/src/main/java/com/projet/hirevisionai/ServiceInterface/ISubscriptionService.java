package com.projet.hirevisionai.ServiceInterface;

import com.projet.hirevisionai.Dto.PaymentDTO;
import com.projet.hirevisionai.Dto.SubscriptionCreateRequestDTO;
import com.projet.hirevisionai.Dto.SubscriptionDTO;
import com.projet.hirevisionai.Dto.SubscriptionStatsDTO;

import java.util.List;

public interface ISubscriptionService {
    SubscriptionStatsDTO getStatsOverview();
    SubscriptionDTO subscribe(SubscriptionCreateRequestDTO request);
    SubscriptionDTO cancel(Long userId);
    List<SubscriptionDTO> getByUserId(Long userId);
    List<PaymentDTO> getPaymentsByUserId(Long userId);
}
