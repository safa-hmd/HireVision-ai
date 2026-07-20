package com.projet.hirevisionai.Controller;

import com.projet.hirevisionai.Dto.AnalyticsDTO;
import com.projet.hirevisionai.ServiceInterface.IAnalyticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import com.projet.hirevisionai.Security.jwt.JwtAuthFilter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AnalyticsController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IAnalyticsService analyticsService;

    @Test
    void getOverview_shouldReturnAnalytics() throws Exception {
        AnalyticsDTO analytics = AnalyticsDTO.builder()
                .totalCvsAnalyzed(50)
                .totalMatchings(30)
                .avgMatchingScore(64.2)
                .cvUploadsLast7Days(List.of())
                .matchingScoreDistribution(List.of())
                .topMissingSkills(List.of())
                .interviewsByDifficulty(List.of())
                .scoreTrendLast6Months(List.of())
                .build();

        when(analyticsService.getOverview()).thenReturn(analytics);

        mockMvc.perform(get("/admin/analytics/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCvsAnalyzed").value(50))
                .andExpect(jsonPath("$.totalMatchings").value(30))
                .andExpect(jsonPath("$.avgMatchingScore").value(64.2));
    }
}
