package com.projet.hirevisionai.Controller;

import com.projet.hirevisionai.Dto.MissedSkillDTO;
import com.projet.hirevisionai.Entity.SkillPriority;
import com.projet.hirevisionai.ServiceInterface.IMissedSkillService;
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

@WebMvcTest(controllers = MissedSkillController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser
class MissedSkillControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IMissedSkillService missedSkillService;

    @Test
    void getByUserId_shouldReturnMissedSkills_whenUserHasSome() throws Exception {
        MissedSkillDTO skill1 = MissedSkillDTO.builder()
                .id(1L).skillName("Docker").priority(SkillPriority.HAUTE)
                .estimatedWeeks(2).matchingResultId(7L).learningPlans(List.of())
                .build();
        MissedSkillDTO skill2 = MissedSkillDTO.builder()
                .id(2L).skillName("Kubernetes").priority(SkillPriority.MOYENNE)
                .estimatedWeeks(4).matchingResultId(7L).learningPlans(List.of())
                .build();

        when(missedSkillService.getByUserId(3L)).thenReturn(List.of(skill1, skill2));

        mockMvc.perform(get("/missed-skills/user/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].skillName").value("Docker"))
                .andExpect(jsonPath("$[0].priority").value("HAUTE"))
                .andExpect(jsonPath("$[1].estimatedWeeks").value(4));
    }

    @Test
    void getByUserId_shouldReturnEmptyList_whenUserHasNoMissedSkills() throws Exception {
        when(missedSkillService.getByUserId(42L)).thenReturn(List.of());

        mockMvc.perform(get("/missed-skills/user/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
