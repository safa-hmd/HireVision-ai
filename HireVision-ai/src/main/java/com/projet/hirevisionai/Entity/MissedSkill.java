package com.projet.hirevisionai.Entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MissedSkill {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    String skillName;

    @Enumerated(EnumType.STRING)
    SkillPriority priority;

    Integer estimatedWeeks;

    @ManyToOne
    @JoinColumn(name = "matching_result_id")
    MatchingResult matchingResult;

    @OneToMany(mappedBy = "missedSkill", cascade = CascadeType.ALL)
    List<LearningPlan> learningPlans;
}