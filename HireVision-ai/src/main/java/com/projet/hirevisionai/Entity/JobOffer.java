package com.projet.hirevisionai.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class JobOffer {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int idJobOffer;
    private String title;
    private String description;


}
