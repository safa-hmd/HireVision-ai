package com.projet.hirevisionai.Entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel. PRIVATE)
public class Interview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDateTime startDate;
    private int durationMinutes;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne @JoinColumn(name = "cv_id")
    private CV cv;

    @OneToMany(mappedBy = "interview", cascade = CascadeType.ALL)
    private List<Question> questions;

    @OneToOne(mappedBy = "interview", cascade = CascadeType.ALL)
    private Feedback feedback;

    @OneToOne(mappedBy = "interview", cascade = CascadeType.ALL)
    private VoiceAnalysis voiceAnalysis;

    @OneToOne(mappedBy = "interview", cascade = CascadeType.ALL)
    private BehaviorAnalysis behaviorAnalysis;
}