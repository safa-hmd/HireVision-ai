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
public class VoiceAnalysis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private float clarityScore;
    private float paceScore;
    private float tonalVariationScore;
    private String audioPath;

    @OneToOne
    @JoinColumn(name = "interview_id")
    private Interview interview;
}
