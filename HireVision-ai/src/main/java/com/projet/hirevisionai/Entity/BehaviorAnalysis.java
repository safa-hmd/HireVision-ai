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
public class BehaviorAnalysis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private float postureScore;
    private float eyeContactScore;
    private float expressionScore;
    private String videoPath;

    @OneToOne
    @JoinColumn(name = "interview_id")
    private Interview interview;
}