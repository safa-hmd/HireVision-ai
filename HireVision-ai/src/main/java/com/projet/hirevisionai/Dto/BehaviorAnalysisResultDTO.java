package com.projet.hirevisionai.Dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BehaviorAnalysisResultDTO {

    @NotNull
    private Long   interviewId;
    private float  postureScore;
    private float  eyeContactScore;
    private float  expressionScore;
    private String videoPath;
}