package com.projet.hirevisionai.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CvAnalysisDTO {

    private List<String> skills;
    private List<EducationEntry> education;
    private List<ExperienceEntry> experience;
    private List<String> certifications;
    private List<LanguageEntry> languages;
    private String summary;
    private String profile;
    private Integer confidence;

    @JsonProperty("global_score")
    private Integer globalScore;

    @JsonProperty("skill_scores")
    private Map<String, Integer> skillScores;

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class EducationEntry {
        private String degree;
        private String institution;
        private String period;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class ExperienceEntry {
        private String title;
        private String company;
        private String period;
        private String description;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class LanguageEntry {
        private String language;
        private String level;
    }
}