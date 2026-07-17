package com.projet.hirevisionai.Entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "plans")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
public class Plan {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "plan_key")
    private PlanType key; // PRO ou PREMIUM

    private String name;
    private double price;
    private String tagline;

    @Builder.Default
    private boolean highlighted = false;

    @ElementCollection
    @CollectionTable(name = "plan_features", joinColumns = @JoinColumn(name = "plan_key"))
    @Column(name = "feature")
    @OrderColumn(name = "position")
    @Builder.Default
    private List<String> features = new ArrayList<>();
}