package com.projet.hirevisionai.Dto;

import com.projet.hirevisionai.Entity.Subscription;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionDTO {
    private Long id;
    private Long userId;
    private String userFullName;
    private String plan;
    private String status;
    private LocalDateTime startDate;
    private LocalDateTime renewalDate;

    public static SubscriptionDTO fromEntity(Subscription s) {
        if (s == null) return null;
        return SubscriptionDTO.builder()
                .id(s.getId())
                .userId(s.getUser() != null ? s.getUser().getIdUser() : null)
                .userFullName(s.getUser() != null ? s.getUser().getFullName() : null)
                .plan(s.getPlan() != null ? s.getPlan().name() : null)
                .status(s.getStatus() != null ? s.getStatus().name() : null)
                .startDate(s.getStartDate())
                .renewalDate(s.getRenewalDate())
                .build();
    }
}
