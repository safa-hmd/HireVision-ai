package com.projet.hirevisionai.Dto;

import com.projet.hirevisionai.Entity.Interview;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewDTO {

    private Long                id;
    private LocalDateTime       startDate;
    private int                 durationMinutes;
    private Long                userId;
    private Long                cvId;
    private List<QuestionDTO>   questions;
    private FeedbackDTO         feedback;
    private VoiceAnalysisDTO    voiceAnalysis;
    private BehaviorAnalysisDTO behaviorAnalysis;

    public static InterviewDTO fromEntity(Interview i) {
        if (i == null) return null;
        return InterviewDTO.builder()
                .id(i.getId())
                .startDate(i.getStartDate())
                .durationMinutes(i.getDurationMinutes())
                .userId(i.getUser() != null ? i.getUser().getIdUser() : null)
                .cvId(i.getCv() != null ? i.getCv().getId() : null)
                .questions(i.getQuestions() != null
                        ? i.getQuestions().stream()
                        .map(QuestionDTO::fromEntity)
                        .collect(Collectors.toList())
                        : List.of())
                .feedback(FeedbackDTO.fromEntity(i.getFeedback()))
                .voiceAnalysis(VoiceAnalysisDTO.fromEntity(i.getVoiceAnalysis()))
                .behaviorAnalysis(BehaviorAnalysisDTO.fromEntity(i.getBehaviorAnalysis()))
                .build();
    }
}