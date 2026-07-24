package com.projet.hirevisionai.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
public class Skill {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String category;

    @JsonIgnore
    @ManyToMany(mappedBy = "skills")
    private List<CV> cvs;

    @JsonIgnore
    @ManyToMany(mappedBy = "requiredSkills")
    private List<JobOffer> jobOffers;
}