package com.projet.hirevisionai.Dto;

import com.projet.hirevisionai.Entity.VoiceAnalysis;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoiceAnalysisDTO {

    private Long   id;
    private float  clarityScore;
    private float  paceScore;
    private float  tonalVariationScore;
    private String audioPath;
    private Long   interviewId;

    public static VoiceAnalysisDTO fromEntity(VoiceAnalysis v) {
        if (v == null) return null;
        return VoiceAnalysisDTO.builder()
                .id(v.getId())
                .clarityScore(v.getClarityScore())
                .paceScore(v.getPaceScore())
                .tonalVariationScore(v.getTonalVariationScore())
                .audioPath(v.getAudioPath())
                .interviewId(v.getInterview() != null ? v.getInterview().getId() : null)
                .build();
    }
}