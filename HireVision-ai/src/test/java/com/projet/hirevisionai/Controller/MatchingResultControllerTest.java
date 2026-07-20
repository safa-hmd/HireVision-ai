package com.projet.hirevisionai.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projet.hirevisionai.Dto.CvDTO;
import com.projet.hirevisionai.Dto.JobMatchRequestDTO;
import com.projet.hirevisionai.Dto.MatchingResultDTO;
import com.projet.hirevisionai.Entity.Role;
import com.projet.hirevisionai.Entity.User;
import com.projet.hirevisionai.Repository.UserRepository;
import com.projet.hirevisionai.ServiceInterface.ICvService;
import com.projet.hirevisionai.ServiceInterface.IMatchingResultService;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = MatchingResultController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class MatchingResultControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IMatchingResultService matchingResultService;

    @MockitoBean
    private ICvService cvService;

    @MockitoBean
    private UserRepository userRepository;

    private User owner(Long id) {
        return User.builder().idUser(id).email("owner@test.com").role(Role.CANDIDATE).build();
    }

    @Test
    @WithMockUser(username = "owner@test.com")
    void getByCvId_shouldReturnResults_whenCvBelongsToRequester() throws Exception {
        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner(1L)));
        when(cvService.getById(10L)).thenReturn(CvDTO.builder().id(10L).userId(1L).build());
        when(matchingResultService.getByCvId(10L)).thenReturn(List.of(
                MatchingResultDTO.builder().id(1L).cvId(10L).score(0.85f).build()
        ));

        mockMvc.perform(get("/matching-results/cv/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cvId").value(10));
    }

    @Test
    @WithMockUser(username = "intruder@test.com")
    void getByCvId_shouldReturnForbidden_whenCvBelongsToAnotherUser() throws Exception {
        when(userRepository.findByEmail("intruder@test.com")).thenReturn(Optional.of(owner(2L)));
        when(cvService.getById(10L)).thenReturn(CvDTO.builder().id(10L).userId(1L).build());

        mockMvc.perform(get("/matching-results/cv/10"))
                .andExpect(status().isForbidden());

        verify(matchingResultService, never()).getByCvId(any());
    }

    @Test
    @WithMockUser(username = "owner@test.com")
    void getByUserId_shouldReturnResults_whenOwnerRequestsOwnData() throws Exception {
        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner(1L)));
        when(matchingResultService.getByUserId(1L)).thenReturn(List.of(
                MatchingResultDTO.builder().id(1L).cvId(10L).score(0.9f).build()
        ));

        mockMvc.perform(get("/matching-results/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @WithMockUser(username = "owner@test.com")
    void getBestByCvId_shouldReturnBestMatch() throws Exception {
        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner(1L)));
        when(cvService.getById(10L)).thenReturn(CvDTO.builder().id(10L).userId(1L).build());
        when(matchingResultService.getBestByCvId(10L)).thenReturn(
                MatchingResultDTO.builder().id(1L).cvId(10L).score(0.95f).build()
        );

        mockMvc.perform(get("/matching-results/cv/10/best"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(0.95));
    }

    @Test
    @WithMockUser(username = "owner@test.com")
    void matchAndSave_shouldReturnMatchingResult_whenCvBelongsToRequester() throws Exception {
        JobMatchRequestDTO request = new JobMatchRequestDTO();
        request.setCvId(10L);
        request.setJobOfferId(3L);

        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner(1L)));
        when(cvService.getById(10L)).thenReturn(CvDTO.builder().id(10L).userId(1L).build());
        when(matchingResultService.matchAndSave(any(JobMatchRequestDTO.class))).thenReturn(
                MatchingResultDTO.builder().id(1L).cvId(10L).jobOfferId(3L).score(0.7f).build()
        );

        mockMvc.perform(post("/matching-results/match")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobOfferId").value(3));
    }

    @Test
    @WithMockUser(username = "intruder@test.com")
    void matchAndSave_shouldReturnForbidden_whenCvBelongsToAnotherUser() throws Exception {
        JobMatchRequestDTO request = new JobMatchRequestDTO();
        request.setCvId(10L);

        when(userRepository.findByEmail("intruder@test.com")).thenReturn(Optional.of(owner(2L)));
        when(cvService.getById(10L)).thenReturn(CvDTO.builder().id(10L).userId(1L).build());

        mockMvc.perform(post("/matching-results/match")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(matchingResultService, never()).matchAndSave(any());
    }
}