package com.projet.hirevisionai.ServiceImpl;

import com.projet.hirevisionai.Dto.*;
import com.projet.hirevisionai.Entity.Interview;
import com.projet.hirevisionai.Entity.Question;
import com.projet.hirevisionai.Entity.User;
import com.projet.hirevisionai.Repository.FeedbackRepository;
import com.projet.hirevisionai.Repository.InterviewRepository;
import com.projet.hirevisionai.Repository.QuestionRepository;
import com.projet.hirevisionai.Repository.UserRepository;
import com.projet.hirevisionai.ServiceInterface.IAdminStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminStatsServiceImpl implements IAdminStatsService {

    private final UserRepository userRepository;
    private final InterviewRepository interviewRepository;
    private final FeedbackRepository feedbackRepository;
    private final QuestionRepository questionRepository;

    @Override
    public DashboardStatsDTO getOverview() {
        LocalDateTime now = LocalDateTime.now();

        // ---- Utilisateurs ----
        long totalUsers = userRepository.count();
        LocalDateTime last30 = now.minusDays(30);
        long newUsersLast30 = userRepository.countByCreatedAtAfter(last30);
        long olderUsers = totalUsers - newUsersLast30;
        double totalUsersChangePercent = olderUsers > 0
                ? (newUsersLast30 * 100.0) / olderUsers
                : (newUsersLast30 > 0 ? 100.0 : 0.0);

        // ---- Entretiens ce mois vs mois dernier ----
        LocalDateTime startOfThisMonth = now.withDayOfMonth(1).toLocalDate().atStartOfDay();
        LocalDateTime startOfLastMonth = startOfThisMonth.minusMonths(1);

        long totalInterviewsThisMonth = interviewRepository.countByStartDateBetween(startOfThisMonth, now);
        long totalInterviewsLastMonth = interviewRepository.countByStartDateBetween(startOfLastMonth, startOfThisMonth);
        double interviewsChangePercent = totalInterviewsLastMonth > 0
                ? ((totalInterviewsThisMonth - totalInterviewsLastMonth) * 100.0) / totalInterviewsLastMonth
                : (totalInterviewsThisMonth > 0 ? 100.0 : 0.0);

        // ---- Score moyen global ----
        Double avgScoreOverall = feedbackRepository.findGlobalAverageScore();
        double avgGlobalScore = avgScoreOverall != null ? avgScoreOverall : 0.0;

        Double avgScoreThisMonth = interviewRepository.findAvgScoreByDateRange(startOfThisMonth, now);
        Double avgScoreLastMonth = interviewRepository.findAvgScoreByDateRange(startOfLastMonth, startOfThisMonth);
        double avgGlobalScoreChangePercent = (avgScoreThisMonth != null && avgScoreLastMonth != null && avgScoreLastMonth > 0)
                ? ((avgScoreThisMonth - avgScoreLastMonth) * 100.0) / avgScoreLastMonth
                : 0.0;

        // ---- Nouveaux utilisateurs, 7 derniers jours ----
        List<DayCountDTO> newUsersLast7Days = buildLast7DaysCounts();

        // ---- Répartition des entretiens par difficulté ----
        List<CategoryCountDTO> interviewsByDifficulty = buildDifficultyDistribution();

        // ---- Derniers inscrits ----
        List<UserDTO> recentUsers = userRepository.findTop5ByOrderByCreatedAtDesc()
                .stream()
                .map(UserDTO::fromEntity)
                .collect(Collectors.toList());

        // ---- Entretiens récents ----
        List<RecentInterviewDTO> recentInterviews = interviewRepository.findTop5ByOrderByStartDateDesc()
                .stream()
                .map(this::toRecentInterviewDTO)
                .collect(Collectors.toList());

        return DashboardStatsDTO.builder()
                .totalUsers(totalUsers)
                .totalUsersChangePercent(round1(totalUsersChangePercent))
                .totalInterviewsThisMonth(totalInterviewsThisMonth)
                .interviewsChangePercent(round1(interviewsChangePercent))
                .avgGlobalScore(round1(avgGlobalScore))
                .avgGlobalScoreChangePercent(round1(avgGlobalScoreChangePercent))
                .newUsersLast7Days(newUsersLast7Days)
                .interviewsByDifficulty(interviewsByDifficulty)
                .recentUsers(recentUsers)
                .recentInterviews(recentInterviews)
                .build();
    }

    private List<DayCountDTO> buildLast7DaysCounts() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.minusDays(6).atStartOfDay();
        List<User> recentUsers = userRepository.findByCreatedAtAfter(start);

        Map<LocalDate, Long> countsByDay = new HashMap<>();
        for (User u : recentUsers) {
            if (u.getCreatedAt() == null) continue;
            LocalDate day = u.getCreatedAt().toLocalDate();
            countsByDay.merge(day, 1L, Long::sum);
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

    private String frenchDifficultyLabel(Question.Difficulty difficulty) {
        if (difficulty == null) return "Non défini";
        switch (difficulty) {
            case EASY: return "Facile";
            case MEDIUM: return "Moyen";
            case HARD: return "Difficile";
            default: return difficulty.name();
        }
    }

    private RecentInterviewDTO toRecentInterviewDTO(Interview interview) {
        Double globalScore = null;
        if (interview.getFeedback() != null) {
            var f = interview.getFeedback();
            globalScore = (double) ((f.getTechnicalScore() + f.getCommunicationScore()
                    + f.getConfidenceScore() + f.getEyeContactScore()) / 4);
            globalScore = round1(globalScore);
        }

        String dominantDifficulty = "-";
        if (interview.getQuestions() != null && !interview.getQuestions().isEmpty()) {
            Map<Question.Difficulty, Long> counts = interview.getQuestions().stream()
                    .filter(q -> q.getDifficulty() != null)
                    .collect(Collectors.groupingBy(Question::getDifficulty, Collectors.counting()));
            Optional<Map.Entry<Question.Difficulty, Long>> top = counts.entrySet().stream()
                    .max(Map.Entry.comparingByValue());
            if (top.isPresent()) {
                dominantDifficulty = frenchDifficultyLabel(top.get().getKey());
            }
        }

        return RecentInterviewDTO.builder()
                .id(interview.getId())
                .candidateName(interview.getUser() != null ? interview.getUser().getFullName() : "Inconnu")
                .difficulty(dominantDifficulty)
                .globalScore(globalScore)
                .durationMinutes(interview.getDurationMinutes())
                .startDate(interview.getStartDate())
                .build();
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
