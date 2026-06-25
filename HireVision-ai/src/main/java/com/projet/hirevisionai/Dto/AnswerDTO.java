package com.projet.hirevisionai.Dto;

import com.projet.hirevisionai.Entity.Answer;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnswerDTO {

    private Long   id;
    private String answerText;
    private float  score;
    private String aiComment;
    private Long   questionId;

    public static AnswerDTO fromEntity(Answer a) {
        if (a == null) return null;
        return AnswerDTO.builder()
                .id(a.getId())
                .answerText(a.getAnswerText())
                .score(a.getScore())
                .aiComment(a.getAiComment())
                .questionId(a.getQuestion() != null ? a.getQuestion().getId() : null)
                .build();
    }
}