package com.projet.hirevisionai.Entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "job_offers")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
public class JobOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String company;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "job_offer_skill",
            joinColumns = @JoinColumn(name = "job_offer_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id"))
    @Builder.Default
    private List<Skill> requiredSkills = new ArrayList<>();

    @OneToMany(mappedBy = "jobOffer")
    private List<MatchingResult> matchingResults;
}
