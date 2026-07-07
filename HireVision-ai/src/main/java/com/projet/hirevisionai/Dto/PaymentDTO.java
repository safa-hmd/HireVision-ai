package com.projet.hirevisionai.Dto;

import com.projet.hirevisionai.Entity.Payment;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentDTO {
    private Long id;
    private Long userId;
    private String userFullName;
    private String plan;
    private double amount;
    private String status;
    private LocalDateTime paymentDate;

    public static PaymentDTO fromEntity(Payment p) {
        if (p == null) return null;
        return PaymentDTO.builder()
                .id(p.getId())
                .userId(p.getUser() != null ? p.getUser().getIdUser() : null)
                .userFullName(p.getUser() != null ? p.getUser().getFullName() : null)
                .plan(p.getPlan() != null ? p.getPlan().name() : null)
                .amount(p.getAmount())
                .status(p.getStatus() != null ? p.getStatus().name() : null)
                .paymentDate(p.getPaymentDate())
                .build();
    }
}
