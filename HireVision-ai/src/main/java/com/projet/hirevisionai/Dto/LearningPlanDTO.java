package com.projet.hirevisionai.Dto;

import com.projet.hirevisionai.Entity.LearningPlan;
import com.projet.hirevisionai.Entity.PlanSource;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearningPlanDTO {

    private Long       id;
    private String     title;
    private String     content;
    private String     resourceUrl;
    private Integer    weekNumber;
    private PlanSource source;
    private Long       missedSkillId;
    private Long       interviewId;

    public static LearningPlanDTO fromEntity(LearningPlan lp) {
        if (lp == null) return null;
        return LearningPlanDTO.builder()
                .id(lp.getId())
                .title(lp.getTitle())
                .content(lp.getContent())
                .resourceUrl(lp.getResourceUrl())
                .weekNumber(lp.getWeekNumber())
                .source(lp.getSource())
                .missedSkillId(lp.getMissedSkill() != null ? lp.getMissedSkill().getId() : null)
                .interviewId(lp.getInterview() != null ? lp.getInterview().getId() : null)
                .build();
    }
}