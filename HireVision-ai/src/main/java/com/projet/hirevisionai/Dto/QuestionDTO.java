package com.projet.hirevisionai.Dto;

import com.projet.hirevisionai.Entity.Question;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionDTO {

    private Long      id;
    private String    content;
    private String    difficulty;
    private Long      interviewId;
    private AnswerDTO answer;

    public static QuestionDTO fromEntity(Question q) {
        if (q == null) return null;
        return QuestionDTO.builder()
                .id(q.getId())
                .content(q.getContent())
                .difficulty(q.getDifficulty() != null ? q.getDifficulty().name() : null)
                .interviewId(q.getInterview() != null ? q.getInterview().getId() : null)
                .answer(AnswerDTO.fromEntity(q.getAnswer()))
                .build();
    }
}