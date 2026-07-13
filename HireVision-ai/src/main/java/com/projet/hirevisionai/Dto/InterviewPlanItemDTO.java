package com.projet.hirevisionai.Dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewPlanItemDTO {
    private Integer semaine;
    private String  theme;
    private String  ressource;
    private String  action;
}