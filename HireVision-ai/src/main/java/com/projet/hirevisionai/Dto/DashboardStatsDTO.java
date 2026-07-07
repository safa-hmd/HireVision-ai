package com.projet.hirevisionai.Dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsDTO {

    private long totalUsers;
    private double totalUsersChangePercent;

    private long totalInterviewsThisMonth;
    private double interviewsChangePercent;

    private double avgGlobalScore; // sur 100
    private double avgGlobalScoreChangePercent;

    private List<DayCountDTO> newUsersLast7Days;
    private List<CategoryCountDTO> interviewsByDifficulty;

    private List<UserDTO> recentUsers;
    private List<RecentInterviewDTO> recentInterviews;
}
