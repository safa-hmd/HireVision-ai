package com.projet.hirevisionai.Dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecentInterviewDTO {
    private Long id;
    private String candidateName;
    private String difficulty; // difficulté dominante des questions de l'entretien
    private Double globalScore; // null si pas encore de feedback
    private int durationMinutes;
    private LocalDateTime startDate;
}
