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
public class Answer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String answerText;
    private float score;
    private String aiComment;

    @OneToOne
    @JoinColumn(name = "question_id")
    private Question question;
}