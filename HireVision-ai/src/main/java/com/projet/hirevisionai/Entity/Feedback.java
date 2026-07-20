package com.projet.hirevisionai.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
public class Feedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private float technicalScore;
    private float communicationScore;
    private float confidenceScore;
    private float eyeContactScore;

    @JsonIgnore
    @OneToOne
    @JoinColumn(name = "interview_id")
    private Interview interview;
}
