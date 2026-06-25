package com.projet.hirevisionai.Dto;

import com.projet.hirevisionai.Entity.Skill;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillDTO {

    private Long   id;
    private String name;
    private String category;

    public static SkillDTO fromEntity(Skill s) {
        if (s == null) return null;
        return SkillDTO.builder()
                .id(s.getId())
                .name(s.getName())
                .category(s.getCategory())
                .build();
    }
}