package com.projet.hirevisionai.ServiceImpl;

import com.projet.hirevisionai.Dto.AnalyticsDTO;
import com.projet.hirevisionai.Entity.MatchingResult;
import com.projet.hirevisionai.Entity.MissedSkill;
import com.projet.hirevisionai.Entity.Question;
import com.projet.hirevisionai.Repository.*;
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
class AnalyticsServiceImplTest {

    @Mock
    private CvRepository cvRepository;
    @Mock
    private MatchingResultRepository matchingResultRepository;
    @Mock
    private MissedSkillRepository missedSkillRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private FeedbackRepository feedbackRepository;

    @InjectMocks
    private AnalyticsServiceImpl analyticsService;

    @Test
    void getOverviewTest_shouldReturnZeroedStats_whenNoDataExists() {
        when(cvRepository.count()).thenReturn(0L);
        when(matchingResultRepository.count()).thenReturn(0L);
        when(matchingResultRepository.findAll()).thenReturn(List.of());
        when(cvRepository.findByUploadDateAfter(any())).thenReturn(List.of());
        when(missedSkillRepository.findAll()).thenReturn(List.of());
        when(questionRepository.countGroupedByDifficulty()).thenReturn(List.of());
        when(feedbackRepository.findAll()).thenReturn(List.of());

        AnalyticsDTO result = analyticsService.getOverview();

        assertThat(result.getTotalCvsAnalyzed()).isZero();
        assertThat(result.getTotalMatchings()).isZero();
        assertThat(result.getAvgMatchingScore()).isZero();
        assertThat(result.getCvUploadsLast7Days()).hasSize(7);
        assertThat(result.getMatchingScoreDistribution()).hasSize(4);
        assertThat(result.getTopMissingSkills()).isEmpty();
        assertThat(result.getScoreTrendLast6Months()).hasSize(6);
    }

    @Test
    void getOverviewTest_shouldComputeAverageMatchingScore_andDistribution() {
        MatchingResult low = MatchingResult.builder().id(1L).score(10f).build();
        MatchingResult high = MatchingResult.builder().id(2L).score(90f).build();

        when(cvRepository.count()).thenReturn(2L);
        when(matchingResultRepository.count()).thenReturn(2L);
        when(matchingResultRepository.findAll()).thenReturn(List.of(low, high));
        when(cvRepository.findByUploadDateAfter(any())).thenReturn(List.of());
        when(missedSkillRepository.findAll()).thenReturn(List.of());
        when(questionRepository.countGroupedByDifficulty()).thenReturn(List.of());
        when(feedbackRepository.findAll()).thenReturn(List.of());

        AnalyticsDTO result = analyticsService.getOverview();

        assertThat(result.getAvgMatchingScore()).isEqualTo(50.0);
        // un score < 25 -> bucket "0-25%", un score >= 75 -> bucket "75-100%"
        assertThat(result.getMatchingScoreDistribution().get(0).getValue()).isEqualTo(1L);
        assertThat(result.getMatchingScoreDistribution().get(3).getValue()).isEqualTo(1L);
    }

    @Test
    void getOverviewTest_shouldRankTopMissingSkillsByFrequency() {
        MissedSkill docker1 = MissedSkill.builder().id(1L).skillName("Docker").build();
        MissedSkill docker2 = MissedSkill.builder().id(2L).skillName("Docker").build();
        MissedSkill k8s = MissedSkill.builder().id(3L).skillName("Kubernetes").build();

        when(cvRepository.count()).thenReturn(0L);
        when(matchingResultRepository.count()).thenReturn(0L);
        when(matchingResultRepository.findAll()).thenReturn(List.of());
        when(cvRepository.findByUploadDateAfter(any())).thenReturn(List.of());
        when(missedSkillRepository.findAll()).thenReturn(List.of(docker1, docker2, k8s));
        when(questionRepository.countGroupedByDifficulty()).thenReturn(List.of());
        when(feedbackRepository.findAll()).thenReturn(List.of());

        AnalyticsDTO result = analyticsService.getOverview();

        assertThat(result.getTopMissingSkills()).hasSize(2);
        assertThat(result.getTopMissingSkills().get(0).getLabel()).isEqualTo("Docker");
        assertThat(result.getTopMissingSkills().get(0).getValue()).isEqualTo(2L);
    }

    @Test
    void getOverviewTest_shouldMapDifficultyDistribution() {
        Object[] row = new Object[]{Question.Difficulty.EASY, 3L};

        when(cvRepository.count()).thenReturn(0L);
        when(matchingResultRepository.count()).thenReturn(0L);
        when(matchingResultRepository.findAll()).thenReturn(List.of());
        when(cvRepository.findByUploadDateAfter(any())).thenReturn(List.of());
        when(missedSkillRepository.findAll()).thenReturn(List.of());
        when(questionRepository.countGroupedByDifficulty()).thenReturn(java.util.Collections.singletonList(row));
        when(feedbackRepository.findAll()).thenReturn(List.of());

        AnalyticsDTO result = analyticsService.getOverview();

        assertThat(result.getInterviewsByDifficulty()).hasSize(1);
        assertThat(result.getInterviewsByDifficulty().get(0).getLabel()).isEqualTo("Facile");
        assertThat(result.getInterviewsByDifficulty().get(0).getValue()).isEqualTo(3L);
    }
}
