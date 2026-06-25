package com.projet.hirevisionai.Dto;

import com.projet.hirevisionai.Entity.LearningPlan;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearningPlanDTO {

    private Long   id;
    private String title;
    private String content;
    private String resourceUrl;
    private Long   missedSkillId;

    public static LearningPlanDTO fromEntity(LearningPlan lp) {
        if (lp == null) return null;
        return LearningPlanDTO.builder()
                .id(lp.getId())
                .title(lp.getTitle())
                .content(lp.getContent())
                .resourceUrl(lp.getResourceUrl())
                .missedSkillId(lp.getMissedSkill() != null ? lp.getMissedSkill().getId() : null)
                .build();
    }
}