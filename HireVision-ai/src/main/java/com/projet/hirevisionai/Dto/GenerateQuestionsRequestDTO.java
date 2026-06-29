package com.projet.hirevisionai.Dto;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenerateQuestionsRequestDTO {
    private List<String> skills;
    private String jobDescription;
    private String candidateName;
    private String profile;
    private Long cvId;
}