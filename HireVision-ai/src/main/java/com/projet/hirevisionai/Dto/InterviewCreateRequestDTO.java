package com.projet.hirevisionai.Dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewCreateRequestDTO {

    @NotNull
    private Long          userId;

    @NotNull
    private Long          cvId;

    private LocalDateTime startDate;
}