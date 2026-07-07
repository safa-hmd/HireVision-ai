package com.projet.hirevisionai.Dto;

import com.projet.hirevisionai.Entity.PlanType;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionCreateRequestDTO {
    private Long userId;
    private PlanType plan;
}
