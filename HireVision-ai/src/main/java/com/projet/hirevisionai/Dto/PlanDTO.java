package com.projet.hirevisionai.Dto;

import com.projet.hirevisionai.Entity.PlanType;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanDTO {
    private PlanType key;
    private String name;
    private double price;
    private String tagline;
    private boolean highlighted;
    private List<String> features;
}
