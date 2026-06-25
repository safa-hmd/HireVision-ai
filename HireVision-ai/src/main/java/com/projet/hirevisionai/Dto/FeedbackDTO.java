package com.projet.hirevisionai.Dto;

import com.projet.hirevisionai.Entity.Feedback;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedbackDTO {

    private Long  id;
    private float technicalScore;
    private float communicationScore;
    private float confidenceScore;
    private float eyeContactScore;
    private Long  interviewId;

    public float getGlobalScore() {
        return (technicalScore + communicationScore + confidenceScore + eyeContactScore) / 4;
    }

    public static FeedbackDTO fromEntity(Feedback f) {
        if (f == null) return null;
        return FeedbackDTO.builder()
                .id(f.getId())
                .technicalScore(f.getTechnicalScore())
                .communicationScore(f.getCommunicationScore())
                .confidenceScore(f.getConfidenceScore())
                .eyeContactScore(f.getEyeContactScore())
                .interviewId(f.getInterview() != null ? f.getInterview().getId() : null)
                .build();
    }
}