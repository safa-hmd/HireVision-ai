package com.projet.hirevisionai.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projet.hirevisionai.Dto.PlanDTO;
import com.projet.hirevisionai.Entity.PlanType;
import com.projet.hirevisionai.ServiceInterface.IPlanService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PlanController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser
class PlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IPlanService planService;

    @Test
    void getAllPlans_shouldReturnAllPlans() throws Exception {
        when(planService.getAllPlans()).thenReturn(List.of(
                PlanDTO.builder().key(PlanType.PRO).name("Pro").price(19.99).build(),
                PlanDTO.builder().key(PlanType.PREMIUM).name("Premium").price(39.99).build()
        ));

        mockMvc.perform(get("/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void updatePlan_shouldReturnUpdatedPlan_whenKeyIsValid() throws Exception {
        PlanDTO input = PlanDTO.builder().name("Pro Plus").price(24.99).build();
        PlanDTO updated = PlanDTO.builder().key(PlanType.PRO).name("Pro Plus").price(24.99).build();
        when(planService.updatePlan(eq(PlanType.PRO), any(PlanDTO.class))).thenReturn(updated);

        mockMvc.perform(put("/plans/PRO")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Pro Plus"));
    }

    @Test
    void updatePlan_shouldReturn500_whenKeyIsInvalid() throws Exception {
        // La conversion @PathVariable PlanType échoue avec MethodArgumentTypeMismatchException,
        // qui n'a pas de handler dédié dans GlobalExceptionHandler -> capturée par le fallback générique -> 500.
        PlanDTO input = PlanDTO.builder().name("Inconnu").build();

        mockMvc.perform(put("/plans/INEXISTANT")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isInternalServerError());
    }
}