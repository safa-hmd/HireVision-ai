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
@FieldDefaults(level = AccessLevel. PRIVATE)
public class MissedSkill {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String skillName;

    @ManyToOne
    @JoinColumn(name = "matching_result_id")
    private MatchingResult matchingResult;

    @OneToMany(mappedBy = "missedSkill", cascade = CascadeType.ALL)
    private List<LearningPlan> learningPlans;
}