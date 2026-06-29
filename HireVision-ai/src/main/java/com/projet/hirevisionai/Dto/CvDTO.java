package com.projet.hirevisionai.Dto;

import com.projet.hirevisionai.Entity.CV;
import com.projet.hirevisionai.Entity.Skill;
import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CvDTO {
    private Long         id;
    private String       filePath;
    private LocalDate    uploadDate;
    private Long         userId;
    private List<String> skillNames;

    // ── Mapping Entity → DTO ──────────────────────────────────────
    public static CvDTO fromEntity(CV cv) {
        return CvDTO.builder()
                .id(cv.getId())
                .filePath(cv.getFilePath())
                .uploadDate(cv.getUploadDate())
                .userId(cv.getUser() != null ? cv.getUser().getIdUser() : null)
                .skillNames(cv.getSkills() != null    // ← Fix ici
                        ? cv.getSkills().stream()
                        .map(Skill::getName)
                        .collect(Collectors.toList())
                        : List.of())
                .build();
    }
}