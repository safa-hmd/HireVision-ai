package com.projet.hirevisionai.Dto;

import com.projet.hirevisionai.Entity.BehaviorAnalysis;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BehaviorAnalysisDTO {

    private Long   id;
    private float  postureScore;
    private float  eyeContactScore;
    private float  expressionScore;
    private String videoPath;
    private Long   interviewId;

    public static BehaviorAnalysisDTO fromEntity(BehaviorAnalysis b) {
        if (b == null) return null;
        return BehaviorAnalysisDTO.builder()
                .id(b.getId())
                .postureScore(b.getPostureScore())
                .eyeContactScore(b.getEyeContactScore())
                .expressionScore(b.getExpressionScore())
                .videoPath(b.getVideoPath())
                .interviewId(b.getInterview() != null ? b.getInterview().getId() : null)
                .build();
    }
}