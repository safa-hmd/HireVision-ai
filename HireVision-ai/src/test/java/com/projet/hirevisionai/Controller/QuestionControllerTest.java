package com.projet.hirevisionai.Controller;

import com.projet.hirevisionai.Dto.QuestionDTO;
import com.projet.hirevisionai.Entity.Question;
import com.projet.hirevisionai.ServiceInterface.IQuestionService;
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

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = QuestionController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser
class QuestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IQuestionService questionService;

    @Test
    void getAll_shouldReturnAllQuestions() throws Exception {
        when(questionService.getAll()).thenReturn(List.of(
                QuestionDTO.builder().id(1L).content("Qu'est-ce que Spring Boot ?").difficulty("EASY").build(),
                QuestionDTO.builder().id(2L).content("Expliquez le polymorphisme").difficulty("MEDIUM").build()
        ));

        mockMvc.perform(get("/questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getByDifficulty_shouldReturnMatchingQuestions() throws Exception {
        when(questionService.getByDifficulty(Question.Difficulty.HARD)).thenReturn(List.of(
                QuestionDTO.builder().id(3L).content("Expliquez le CAP theorem").difficulty("HARD").build()
        ));

        mockMvc.perform(get("/questions/difficulty/hard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].difficulty").value("HARD"));
    }

    @Test
    void getByDifficulty_shouldReturnBadRequest_whenDifficultyIsInvalid() throws Exception {
        // Question.Difficulty.valueOf() lève IllegalArgumentException pour une valeur inconnue,
        // interceptée par GlobalExceptionHandler.handleIllegalArgs() -> 400.
        mockMvc.perform(get("/questions/difficulty/impossible"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_shouldReturnNoContent_whenQuestionExists() throws Exception {
        doNothing().when(questionService).delete(1L);

        mockMvc.perform(delete("/questions/1"))
                .andExpect(status().isNoContent());

        verify(questionService).delete(1L);
    }
}