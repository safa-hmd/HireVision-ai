package com.projet.hirevisionai.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CV {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    String filePath;
    LocalDate uploadDate;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "user_id")
    User user;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "cv_skill",
            joinColumns = @JoinColumn(name = "cv_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id"))
    List<Skill> skills = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "cv", cascade = CascadeType.ALL, orphanRemoval = true)
    List<MatchingResult> matchingResults = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String analysisJson;

}