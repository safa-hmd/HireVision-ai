package com.projet.hirevisionai.Dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsDTO {

    private long   totalCvsAnalyzed;
    private long   totalMatchings;
    private double avgMatchingScore;

    /** Uploads de CV, 7 derniers jours */
    private List<DayCountDTO> cvUploadsLast7Days;

    /** Répartition des scores de matching (0-25 / 25-50 / 50-75 / 75-100) */
    private List<CategoryCountDTO> matchingScoreDistribution;

    /** Top 5 des compétences les plus fréquemment manquantes */
    private List<CategoryCountDTO> topMissingSkills;

    /** Répartition des questions d'entretien par difficulté */
    private List<CategoryCountDTO> interviewsByDifficulty;

    /** Évolution du score moyen d'entretien, 6 derniers mois */
    private List<ScorePointDTO> scoreTrendLast6Months;
}
