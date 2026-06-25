package com.projet.hirevisionai.Entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel. PRIVATE)
public class LearningPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String content;
    private String resourceUrl;

    @ManyToOne
    @JoinColumn(name = "missed_skill_id")
    private MissedSkill missedSkill;
}
