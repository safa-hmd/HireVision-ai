package com.projet.hirevisionai.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projet.hirevisionai.Dto.JobOfferDTO;
import com.projet.hirevisionai.ServiceInterface.IJobOfferService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = JobOfferController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser
class JobOfferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IJobOfferService jobOfferService;

    @Test
    void create_shouldReturnCreatedOffer() throws Exception {
        JobOfferDTO input = JobOfferDTO.builder().title("Développeur Java").company("HireVision").build();
        JobOfferDTO saved = JobOfferDTO.builder().id(1L).title("Développeur Java").company("HireVision").active(true).build();
        when(jobOfferService.create(any(JobOfferDTO.class))).thenReturn(saved);

        mockMvc.perform(post("/job-offers")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Développeur Java"));
    }

    @Test
    void getById_shouldReturnOffer_whenExists() throws Exception {
        JobOfferDTO offer = JobOfferDTO.builder().id(1L).title("Développeur Java").build();
        when(jobOfferService.getById(1L)).thenReturn(offer);

        mockMvc.perform(get("/job-offers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Développeur Java"));
    }

    @Test
    void getById_shouldReturn500_whenOfferNotFound() throws Exception {
        when(jobOfferService.getById(99L)).thenThrow(new RuntimeException("Offre d'emploi introuvable : 99"));

        mockMvc.perform(get("/job-offers/99"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getAll_shouldReturnAllOffers() throws Exception {
        when(jobOfferService.getAll()).thenReturn(List.of(
                JobOfferDTO.builder().id(1L).title("Offre 1").build(),
                JobOfferDTO.builder().id(2L).title("Offre 2").build()
        ));

        mockMvc.perform(get("/job-offers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getActive_shouldReturnOnlyActiveOffers() throws Exception {
        when(jobOfferService.getActive()).thenReturn(List.of(
                JobOfferDTO.builder().id(1L).title("Offre active").active(true).build()
        ));

        mockMvc.perform(get("/job-offers/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    void search_shouldReturnMatchingOffers() throws Exception {
        when(jobOfferService.search("java")).thenReturn(List.of(
                JobOfferDTO.builder().id(1L).title("Développeur Java").build()
        ));

        mockMvc.perform(get("/job-offers/search").param("keyword", "java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Développeur Java"));
    }

    @Test
    void update_shouldReturnUpdatedOffer_whenExists() throws Exception {
        JobOfferDTO input = JobOfferDTO.builder().title("Titre modifié").build();
        JobOfferDTO updated = JobOfferDTO.builder().id(1L).title("Titre modifié").build();
        when(jobOfferService.update(eq(1L), any(JobOfferDTO.class))).thenReturn(updated);

        mockMvc.perform(put("/job-offers/1")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Titre modifié"));
    }

    @Test
    void delete_shouldReturnNoContent_whenOfferExists() throws Exception {
        doNothing().when(jobOfferService).delete(1L);

        mockMvc.perform(delete("/job-offers/1"))
                .andExpect(status().isNoContent());

        verify(jobOfferService).delete(1L);
    }
}