package com.projet.hirevisionai.Dto;

import com.projet.hirevisionai.Entity.JobOffer;
import com.projet.hirevisionai.Entity.Skill;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobOfferDTO {

    private Long id;
    private String title;
    private String company;
    private String description;
    private boolean active;
    private LocalDateTime createdAt;

    /** Noms des compétences requises (ex: ["Java", "Angular", "Spring Boot"]) */
    private List<String> requiredSkills;

    public static JobOfferDTO fromEntity(JobOffer offer) {
        if (offer == null) return null;
        return JobOfferDTO.builder()
                .id(offer.getId())
                .title(offer.getTitle())
                .company(offer.getCompany())
                .description(offer.getDescription())
                .active(offer.isActive())
                .createdAt(offer.getCreatedAt())
                .requiredSkills(offer.getRequiredSkills() != null
                        ? offer.getRequiredSkills().stream().map(Skill::getName).collect(Collectors.toList())
                        : List.of())
                .build();
    }
}
