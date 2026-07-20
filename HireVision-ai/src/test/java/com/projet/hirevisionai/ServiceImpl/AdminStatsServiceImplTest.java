package com.projet.hirevisionai.ServiceImpl;

import com.projet.hirevisionai.Dto.DashboardStatsDTO;
import com.projet.hirevisionai.Entity.Question;
import com.projet.hirevisionai.Entity.User;
import com.projet.hirevisionai.Repository.FeedbackRepository;
import com.projet.hirevisionai.Repository.InterviewRepository;
import com.projet.hirevisionai.Repository.QuestionRepository;
import com.projet.hirevisionai.Repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminStatsServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private InterviewRepository interviewRepository;
    @Mock
    private FeedbackRepository feedbackRepository;
    @Mock
    private QuestionRepository questionRepository;

    @InjectMocks
    private AdminStatsServiceImpl adminStatsService;

    @Test
    void getOverview_shouldReturnZeroedStats_whenNoDataExists() {
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.countByCreatedAtAfter(any())).thenReturn(0L);
        when(interviewRepository.countByStartDateBetween(any(), any())).thenReturn(0L);
        when(feedbackRepository.findGlobalAverageScore()).thenReturn(null);
        when(interviewRepository.findAvgScoreByDateRange(any(), any())).thenReturn(null);
        when(userRepository.findByCreatedAtAfter(any())).thenReturn(List.of());
        when(questionRepository.countGroupedByDifficulty()).thenReturn(List.of());
        when(userRepository.findTop5ByOrderByCreatedAtDesc()).thenReturn(List.of());
        when(interviewRepository.findTop5ByOrderByStartDateDesc()).thenReturn(List.of());

        DashboardStatsDTO result = adminStatsService.getOverview();

        assertThat(result.getTotalUsers()).isZero();
        assertThat(result.getAvgGlobalScore()).isZero();
        assertThat(result.getNewUsersLast7Days()).hasSize(7);
        assertThat(result.getRecentUsers()).isEmpty();
        assertThat(result.getRecentInterviews()).isEmpty();
    }

    @Test
    void getOverview_shouldComputeUserGrowthPercent() {
        when(userRepository.count()).thenReturn(20L);
        when(userRepository.countByCreatedAtAfter(any())).thenReturn(5L);
        when(interviewRepository.countByStartDateBetween(any(), any())).thenReturn(0L);
        when(feedbackRepository.findGlobalAverageScore()).thenReturn(70.0);
        when(interviewRepository.findAvgScoreByDateRange(any(), any())).thenReturn(null);
        when(userRepository.findByCreatedAtAfter(any())).thenReturn(List.of());
        when(questionRepository.countGroupedByDifficulty()).thenReturn(List.of());
        when(userRepository.findTop5ByOrderByCreatedAtDesc()).thenReturn(List.of());
        when(interviewRepository.findTop5ByOrderByStartDateDesc()).thenReturn(List.of());

        DashboardStatsDTO result = adminStatsService.getOverview();

        // 5 nouveaux sur 15 anciens = 33.3%
        assertThat(result.getTotalUsersChangePercent()).isEqualTo(33.3);
        assertThat(result.getAvgGlobalScore()).isEqualTo(70.0);
    }

    @Test
    void getOverview_shouldIncludeRecentUsersAndInterviews() {
        User user = User.builder().idUser(1L).fullName("Jean Dupont").build();

        when(userRepository.count()).thenReturn(1L);
        when(userRepository.countByCreatedAtAfter(any())).thenReturn(1L);
        when(interviewRepository.countByStartDateBetween(any(), any())).thenReturn(0L);
        when(feedbackRepository.findGlobalAverageScore()).thenReturn(null);
        when(interviewRepository.findAvgScoreByDateRange(any(), any())).thenReturn(null);
        when(userRepository.findByCreatedAtAfter(any())).thenReturn(List.of());
        when(questionRepository.countGroupedByDifficulty())
                .thenReturn(List.of(new Object[]{Question.Difficulty.HARD, 2L}));
        when(userRepository.findTop5ByOrderByCreatedAtDesc()).thenReturn(List.of(user));
        when(interviewRepository.findTop5ByOrderByStartDateDesc()).thenReturn(List.of());

        DashboardStatsDTO result = adminStatsService.getOverview();

        assertThat(result.getRecentUsers()).hasSize(1);
        assertThat(result.getRecentUsers().get(0).getFullName()).isEqualTo("Jean Dupont");
        assertThat(result.getInterviewsByDifficulty()).hasSize(1);
        assertThat(result.getInterviewsByDifficulty().get(0).getLabel()).isEqualTo("Difficile");
    }
}
