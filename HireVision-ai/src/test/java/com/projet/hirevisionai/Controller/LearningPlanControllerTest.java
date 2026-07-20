package com.projet.hirevisionai.Controller;

import com.projet.hirevisionai.Dto.LearningPlanDTO;
import com.projet.hirevisionai.Entity.PlanSource;
import com.projet.hirevisionai.ServiceInterface.ILearningPlanService;
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

@WebMvcTest(controllers = LearningPlanController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser
class LearningPlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ILearningPlanService learningPlanService;

    @Test
    void getByUserId_shouldReturnLearningPlans_whenUserHasPlans() throws Exception {
        LearningPlanDTO plan1 = LearningPlanDTO.builder()
                .id(1L).title("Revoir Spring Boot").content("Approfondir les bases")
                .resourceUrl("https://spring.io").weekNumber(1)
                .source(PlanSource.JOB_MATCHING).missedSkillId(10L).interviewId(20L)
                .build();
        LearningPlanDTO plan2 = LearningPlanDTO.builder()
                .id(2L).title("Projet pratique").content("Coder un mini-projet")
                .resourceUrl("https://github.com").weekNumber(2)
                .source(PlanSource.INTERVIEW).missedSkillId(11L).interviewId(20L)
                .build();

        when(learningPlanService.getByUserId(5L)).thenReturn(List.of(plan1, plan2));

        mockMvc.perform(get("/learning-plans/user/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Revoir Spring Boot"))
                .andExpect(jsonPath("$[1].weekNumber").value(2));
    }

    @Test
    void getByUserId_shouldReturnEmptyList_whenUserHasNoPlans() throws Exception {
        when(learningPlanService.getByUserId(99L)).thenReturn(List.of());

        mockMvc.perform(get("/learning-plans/user/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
