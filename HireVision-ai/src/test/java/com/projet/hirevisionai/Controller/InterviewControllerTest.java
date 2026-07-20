package com.projet.hirevisionai.Controller;

import com.projet.hirevisionai.Dto.InterviewDTO;
import com.projet.hirevisionai.Dto.RecentInterviewDTO;
import com.projet.hirevisionai.ServiceInterface.IInterviewService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = InterviewController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser
class InterviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IInterviewService interviewService;

    @Test
    void getAll_shouldReturnAllInterviews_forAdminView() throws Exception {
        when(interviewService.getAllForAdmin()).thenReturn(List.of(
                RecentInterviewDTO.builder().id(1L).candidateName("Adem Ben").durationMinutes(30).build()
        ));

        mockMvc.perform(get("/interviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].candidateName").value("Adem Ben"));
    }

    @Test
    void getById_shouldReturnInterview_whenExists() throws Exception {
        when(interviewService.getById(1L)).thenReturn(
                InterviewDTO.builder().id(1L).durationMinutes(45).userId(1L).build()
        );

        mockMvc.perform(get("/interviews/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durationMinutes").value(45));
    }

    @Test
    void getById_shouldReturn500_whenInterviewNotFound() throws Exception {
        when(interviewService.getById(99L)).thenThrow(new RuntimeException("Interview introuvable : 99"));

        mockMvc.perform(get("/interviews/99"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getByUserId_shouldReturnUserInterviews() throws Exception {
        when(interviewService.getByUserId(1L)).thenReturn(List.of(
                InterviewDTO.builder().id(1L).userId(1L).build(),
                InterviewDTO.builder().id(2L).userId(1L).build()
        ));

        mockMvc.perform(get("/interviews/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getSortedByDate_shouldReturnSortedInterviews() throws Exception {
        when(interviewService.getByUserIdSortedByDate(1L)).thenReturn(List.of(
                InterviewDTO.builder().id(2L).userId(1L).build(),
                InterviewDTO.builder().id(1L).userId(1L).build()
        ));

        mockMvc.perform(get("/interviews/user/1/sorted"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2));
    }

    @Test
    void getByDateRange_shouldReturnInterviewsWithinRange() throws Exception {
        when(interviewService.getByDateRange(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(InterviewDTO.builder().id(1L).userId(1L).build()));

        mockMvc.perform(get("/interviews/user/1/range")
                        .param("start", "2026-01-01T00:00:00")
                        .param("end", "2026-01-31T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void countByUserId_shouldReturnCount() throws Exception {
        when(interviewService.countByUserId(1L)).thenReturn(5L);

        mockMvc.perform(get("/interviews/user/1/count"))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }

    @Test
    void delete_shouldReturnNoContent_whenInterviewExists() throws Exception {
        doNothing().when(interviewService).delete(1L);

        mockMvc.perform(delete("/interviews/1"))
                .andExpect(status().isNoContent());

        verify(interviewService).delete(1L);
    }
}