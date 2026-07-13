package com.projet.hirevisionai.Dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScorePointDTO {
    private String label;
    private double avgScore;
}
