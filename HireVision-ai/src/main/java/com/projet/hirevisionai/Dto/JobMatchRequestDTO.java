package com.projet.hirevisionai.Dto;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobMatchRequestDTO {
    private Long cvId;
    private List<String> cvSkills;
    private List<String> jobSkills;

    /** Optionnel : si fourni, les compétences requises sont lues depuis l'offre au lieu de jobSkills */
    private Long jobOfferId;
}