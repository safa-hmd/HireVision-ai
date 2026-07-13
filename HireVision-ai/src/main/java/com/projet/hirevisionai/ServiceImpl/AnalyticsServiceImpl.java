package com.projet.hirevisionai.ServiceImpl;

import com.projet.hirevisionai.Dto.*;
import com.projet.hirevisionai.Entity.Feedback;
import com.projet.hirevisionai.Entity.MatchingResult;
import com.projet.hirevisionai.Entity.MissedSkill;
import com.projet.hirevisionai.Entity.Question;
import com.projet.hirevisionai.Repository.*;
import com.projet.hirevisionai.ServiceInterface.IAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements IAnalyticsService {

    private final CvRepository            cvRepository;
    private final MatchingResultRepository matchingResultRepository;
    private final MissedSkillRepository    missedSkillRepository;
    private final QuestionRepository       questionRepository;
    private final FeedbackRepository       feedbackRepository;

    @Override
    public AnalyticsDTO getOverview() {
        return AnalyticsDTO.builder()
                .totalCvsAnalyzed(cvRepository.count())
                .totalMatchings(matchingResultRepository.count())
                .avgMatchingScore(round1(avgMatchingScore()))
                .cvUploadsLast7Days(buildCvUploadsLast7Days())
                .matchingScoreDistribution(buildMatchingScoreDistribution())
                .topMissingSkills(buildTopMissingSkills())
                .interviewsByDifficulty(buildDifficultyDistribution())
                .scoreTrendLast6Months(buildScoreTrendLast6Months())
                .build();
    }

    private double avgMatchingScore() {
        List<MatchingResult> all = matchingResultRepository.findAll();
        if (all.isEmpty()) return 0.0;
        double sum = 0;
        for (MatchingResult mr : all) sum += mr.getScore();
        return sum / all.size();
    }

    private List<DayCountDTO> buildCvUploadsLast7Days() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(6);
        List<com.projet.hirevisionai.Entity.CV> recentCvs = cvRepository.findByUploadDateAfter(start.minusDays(1));

        Map<LocalDate, Long> countsByDay = new HashMap<>();
        for (var cv : recentCvs) {
            if (cv.getUploadDate() == null) continue;
            countsByDay.merge(cv.getUploadDate(), 1L, Long::sum);
        }

        List<DayCountDTO> result = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            String label = day.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.FRENCH);
            label = label.substring(0, 1).toUpperCase() + label.substring(1);
            long count = countsByDay.getOrDefault(day, 0L);
            result.add(DayCountDTO.builder().label(label).value(count).build());
        }
        return result;
    }

    private List<CategoryCountDTO> buildMatchingScoreDistribution() {
        List<MatchingResult> all = matchingResultRepository.findAll();
        long b0 = 0, b1 = 0, b2 = 0, b3 = 0;
        for (MatchingResult mr : all) {
            float s = mr.getScore();
            if (s < 25) b0++;
            else if (s < 50) b1++;
            else if (s < 75) b2++;
            else b3++;
        }
        return List.of(
                CategoryCountDTO.builder().label("0-25%").value(b0).build(),
                CategoryCountDTO.builder().label("25-50%").value(b1).build(),
                CategoryCountDTO.builder().label("50-75%").value(b2).build(),
                CategoryCountDTO.builder().label("75-100%").value(b3).build()
        );
    }

    private List<CategoryCountDTO> buildTopMissingSkills() {
        List<MissedSkill> all = missedSkillRepository.findAll();
        Map<String, Long> counts = all.stream()
                .filter(ms -> ms.getSkillName() != null)
                .collect(Collectors.groupingBy(MissedSkill::getSkillName, Collectors.counting()));

        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(e -> CategoryCountDTO.builder().label(e.getKey()).value(e.getValue()).build())
                .collect(Collectors.toList());
    }

    private List<CategoryCountDTO> buildDifficultyDistribution() {
        List<Object[]> rows = questionRepository.countGroupedByDifficulty();
        List<CategoryCountDTO> result = new ArrayList<>();
        for (Object[] row : rows) {
            Question.Difficulty difficulty = (Question.Difficulty) row[0];
            Long count = (Long) row[1];
            result.add(CategoryCountDTO.builder()
                    .label(frenchDifficultyLabel(difficulty))
                    .value(count)
                    .build());
        }
        return result;
    }

    private List<ScorePointDTO> buildScoreTrendLast6Months() {
        List<Feedback> feedbacks = feedbackRepository.findAll();

        YearMonth current = YearMonth.now();
        List<YearMonth> months = new ArrayList<>();
        for (int i = 5; i >= 0; i--) months.add(current.minusMonths(i));

        Map<YearMonth, List<Double>> scoresByMonth = new HashMap<>();
        for (Feedback f : feedbacks) {
            if (f.getInterview() == null || f.getInterview().getStartDate() == null) continue;
            YearMonth ym = YearMonth.from(f.getInterview().getStartDate());
            double score = (f.getTechnicalScore() + f.getCommunicationScore()
                    + f.getConfidenceScore() + f.getEyeContactScore()) / 4.0;
            scoresByMonth.computeIfAbsent(ym, k -> new ArrayList<>()).add(score);
        }

        List<ScorePointDTO> result = new ArrayList<>();
        for (YearMonth ym : months) {
            String label = ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.FRENCH) + " " + ym.getYear();
            label = label.substring(0, 1).toUpperCase() + label.substring(1);
            List<Double> scores = scoresByMonth.getOrDefault(ym, List.of());
            double avg = scores.isEmpty() ? 0.0 : scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            result.add(ScorePointDTO.builder().label(label).avgScore(round1(avg)).build());
        }
        return result;
    }

    private String frenchDifficultyLabel(Question.Difficulty difficulty) {
        if (difficulty == null) return "Non défini";
        switch (difficulty) {
            case EASY: return "Facile";
            case MEDIUM: return "Moyen";
            case HARD: return "Difficile";
            default: return difficulty.name();
        }
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
