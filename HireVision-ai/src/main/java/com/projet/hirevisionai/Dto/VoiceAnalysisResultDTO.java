package com.projet.hirevisionai.Dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoiceAnalysisResultDTO {

    @NotNull
    private Long   interviewId;
    private float  clarityScore;
    private float  paceScore;
    private float  tonalVariationScore;
    private String audioPath;
}