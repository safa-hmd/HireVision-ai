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
public class MatchingResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private float score;

    @ManyToOne
    @JoinColumn(name = "cv_id")
    private CV cv;

    @OneToMany(mappedBy = "matchingResult", cascade = CascadeType.ALL)
    private List<MissedSkill> missedSkills;
}
