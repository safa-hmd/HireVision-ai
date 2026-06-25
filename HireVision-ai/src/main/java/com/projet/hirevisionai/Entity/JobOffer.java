package com.projet.hirevisionai.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel. PRIVATE)
@Getter
@Setter
public class JobOffer {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int idJobOffer;
    private String title;
    private String description;


}
