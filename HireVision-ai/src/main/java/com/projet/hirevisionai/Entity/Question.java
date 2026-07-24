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
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String content;

    @Enumerated(EnumType.STRING)
    private Difficulty difficulty;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "interview_id")
    private Interview interview;

    @OneToOne(mappedBy = "question", cascade = CascadeType.ALL)
    private Answer answer;

    public enum Difficulty {
        EASY, MEDIUM, HARD
    }
}