package com.projet.hirevisionai.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projet.hirevisionai.Dto.SettingsDTO;
import com.projet.hirevisionai.ServiceInterface.ISettingsService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SettingsController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser
class SettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ISettingsService settingsService;

    @Test
    void getSettings_shouldReturnCurrentSettings() throws Exception {
        SettingsDTO settings = SettingsDTO.builder().platformName("HireVision AI").maxCvSizeMb(10).build();
        when(settingsService.getSettings()).thenReturn(settings);

        mockMvc.perform(get("/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.platformName").value("HireVision AI"));
    }

    @Test
    void updateSettings_shouldReturnUpdatedSettings() throws Exception {
        SettingsDTO input = SettingsDTO.builder().platformName("HireVision Pro").maxCvSizeMb(15).build();
        SettingsDTO updated = SettingsDTO.builder().platformName("HireVision Pro").maxCvSizeMb(15).build();
        when(settingsService.updateSettings(any(SettingsDTO.class))).thenReturn(updated);

        mockMvc.perform(put("/settings")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.platformName").value("HireVision Pro"))
                .andExpect(jsonPath("$.maxCvSizeMb").value(15));
    }
}