package com.projet.hirevisionai.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionCreateRequestDTO {

    @NotBlank
    private String content;

    @NotBlank
    private String difficulty;

    @NotNull
    private Long interviewId;
}