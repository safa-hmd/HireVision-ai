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
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LearningPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String content;
    private String resourceUrl;
    private Integer weekNumber;

    @Enumerated(EnumType.STRING)
    private PlanSource source;

    // Rempli seulement si source = JOB_MATCHING
    @ManyToOne
    @JoinColumn(name = "missed_skill_id")
    private MissedSkill missedSkill;

    // Rempli seulement si source = INTERVIEW
    @ManyToOne
    @JoinColumn(name = "interview_id")
    private Interview interview;
}