package com.projet.hirevisionai.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Setter
@Getter
public class Interview {
    @Id
    private int idInterview;
    private LocalDateTime date;
    private String title;
    private String status;

}
