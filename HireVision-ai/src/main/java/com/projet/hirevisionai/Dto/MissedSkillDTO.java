package com.projet.hirevisionai.Dto;

import com.projet.hirevisionai.Entity.MissedSkill;
import com.projet.hirevisionai.Entity.SkillPriority;
import lombok.*;

import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MissedSkillDTO {

    private Long              id;
    private String            skillName;
    private SkillPriority     priority;
    private Integer           estimatedWeeks;
    private Long              matchingResultId;
    private List<LearningPlanDTO> learningPlans;

    public static MissedSkillDTO fromEntity(MissedSkill ms) {
        if (ms == null) return null;
        return MissedSkillDTO.builder()
                .id(ms.getId())
                .skillName(ms.getSkillName())
                .priority(ms.getPriority())
                .estimatedWeeks(ms.getEstimatedWeeks())
                .matchingResultId(ms.getMatchingResult() != null ? ms.getMatchingResult().getId() : null)
                .learningPlans(ms.getLearningPlans() != null
                        ? ms.getLearningPlans().stream()
                        .map(LearningPlanDTO::fromEntity)
                        .collect(Collectors.toList())
                        : List.of())
                .build();
    }
}