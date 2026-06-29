package com.projet.hirevisionai.Dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CvUploadResponseDTO {
    private CvDTO cv;
    private CvAnalysisDTO analysis;
}