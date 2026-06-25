package com.projet.hirevisionai.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnswerSubmitRequestDTO {

    @NotNull
    private Long   questionId;

    @NotBlank
    private String answerText;
}