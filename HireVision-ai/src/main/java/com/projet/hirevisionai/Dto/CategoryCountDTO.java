package com.projet.hirevisionai.Dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryCountDTO {
    private String label;
    private long value;
}
