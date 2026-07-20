package com.projet.hirevisionai.Controller;

import com.projet.hirevisionai.Dto.DashboardStatsDTO;
import com.projet.hirevisionai.ServiceInterface.IAdminStatsService;
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

@WebMvcTest(controllers = AdminStatsController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser
class AdminStatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IAdminStatsService adminStatsService;

    @Test
    void getOverview_shouldReturnDashboardStats() throws Exception {
        DashboardStatsDTO stats = DashboardStatsDTO.builder()
                .totalUsers(120)
                .totalUsersChangePercent(5.5)
                .totalInterviewsThisMonth(34)
                .interviewsChangePercent(12.0)
                .avgGlobalScore(78.5)
                .avgGlobalScoreChangePercent(2.3)
                .newUsersLast7Days(List.of())
                .interviewsByDifficulty(List.of())
                .recentUsers(List.of())
                .recentInterviews(List.of())
                .build();

        when(adminStatsService.getOverview()).thenReturn(stats);

        mockMvc.perform(get("/admin/stats/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(120))
                .andExpect(jsonPath("$.totalInterviewsThisMonth").value(34))
                .andExpect(jsonPath("$.avgGlobalScore").value(78.5));
    }
}
