package com.projet.hirevisionai.Controller;

import com.projet.hirevisionai.Entity.Interview;
import com.projet.hirevisionai.Entity.Question;
import com.projet.hirevisionai.Repository.InterviewRepository;
import com.projet.hirevisionai.Repository.QuestionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import com.projet.hirevisionai.Security.jwt.JwtAuthFilter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = InterviewAIProxyController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser
class InterviewAIProxyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RestTemplate restTemplate;

    @MockitoBean
    private InterviewRepository interviewRepository;

    @MockitoBean
    private QuestionRepository questionRepository;

    // ── /interview/questions/{specialtyId} ────────────────────────────────

    @Test
    void getQuestions_shouldReturnQuestions_whenPythonServiceIsUp() throws Exception {
        Map<String, Object> body = Map.of("questions", List.of("Q1", "Q2"));
        when(restTemplate.getForEntity(any(URI.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(body));

        mockMvc.perform(get("/interview/questions/java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions.length()").value(2));
    }

    @Test
    void getQuestions_shouldExcludeAlreadyAskedQuestions_whenUserIdProvided() throws Exception {
        Interview interview = Interview.builder().id(100L).build();
        Question q1 = Question.builder().id(1L).content("Qu'est-ce que Spring ?").build();

        when(interviewRepository.findByUserIdUserOrderByStartDateDesc(5L))
                .thenReturn(List.of(interview));
        when(questionRepository.findByInterviewId(100L)).thenReturn(List.of(q1));
        when(restTemplate.getForEntity(any(URI.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("questions", List.of("Q3"))));

        mockMvc.perform(get("/interview/questions/java").param("userId", "5"))
                .andExpect(status().isOk());
    }

    @Test
    void getQuestions_shouldReturn503WithFallback_whenPythonServiceUnavailable() throws Exception {
        when(restTemplate.getForEntity(any(URI.class), eq(Map.class)))
                .thenThrow(new RestClientException("connection refused"));

        mockMvc.perform(get("/interview/questions/java"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.questions.length()").value(0));
    }

    // ── /interview/analyze-voice ───────────────────────────────────────────

    @Test
    void analyzeVoice_shouldReturnPythonResponse_whenAvailable() throws Exception {
        Map<String, Object> body = Map.of("score_global", 90);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(body));

        mockMvc.perform(post("/interview/analyze-voice")
                        .contentType("application/json")
                        .content("{\"transcript\":\"bonjour\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score_global").value(90));
    }

    @Test
    void analyzeVoice_shouldReturnFallbackScores_whenPythonServiceFails() throws Exception {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RestClientException("timeout"));

        mockMvc.perform(post("/interview/analyze-voice")
                        .contentType("application/json")
                        .content("{\"transcript\":\"bonjour\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score_global").value(70))
                .andExpect(jsonPath("$.niveau").value("Bien"));
    }

    // ── /interview/analyze-frame ───────────────────────────────────────────

    @Test
    void analyzeFrame_shouldReturnPythonResponse_whenAvailable() throws Exception {
        MockMultipartFile frame = new MockMultipartFile("frame", "frame.jpg", "image/jpeg", "bytes".getBytes());
        Map<String, Object> body = Map.of("eye_contact", 95, "face_detected", true);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(body));

        mockMvc.perform(multipart("/interview/analyze-frame").file(frame))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eye_contact").value(95));
    }

    @Test
    void analyzeFrame_shouldReturnFallbackMetrics_whenPythonServiceFails() throws Exception {
        MockMultipartFile frame = new MockMultipartFile("frame", "frame.jpg", "image/jpeg", "bytes".getBytes());
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RestClientException("timeout"));

        mockMvc.perform(multipart("/interview/analyze-frame").file(frame))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.face_detected").value(false));
    }

    // ── /interview/feedback ────────────────────────────────────────────────

    @Test
    void generateFeedback_shouldReturnPythonResponse_whenAvailable() throws Exception {
        Map<String, Object> body = Map.of("titre", "Rapport IA");
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(body));

        mockMvc.perform(post("/interview/feedback")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titre").value("Rapport IA"));
    }

    @Test
    void generateFeedback_shouldReturnFallbackReport_whenPythonServiceFails() throws Exception {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RestClientException("down"));

        mockMvc.perform(post("/interview/feedback")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titre").value("Rapport généré"))
                .andExpect(jsonPath("$.plan_apprentissage.length()").value(3));
    }

    // ── /interview/next-question ───────────────────────────────────────────

    @Test
    void getNextQuestion_shouldReturnPythonResponse_whenAvailable() throws Exception {
        Map<String, Object> body = Map.of("question", "Expliquez le polymorphisme", "finished", false);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(body));

        mockMvc.perform(post("/interview/next-question")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.finished").value(false));
    }

    @Test
    void getNextQuestion_shouldReturn503_whenPythonServiceFails() throws Exception {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RestClientException("down"));

        mockMvc.perform(post("/interview/next-question")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.finished").value(false));
    }

    // ── /interview/specialties ─────────────────────────────────────────────

    @Test
    void getSpecialties_shouldReturnPythonResponse_whenAvailable() throws Exception {
        when(restTemplate.getForEntity(anyString(), eq(Object.class)))
                .thenReturn(ResponseEntity.ok(List.of("java", "python")));

        mockMvc.perform(get("/interview/specialties"))
                .andExpect(status().isOk());
    }

    @Test
    void getSpecialties_shouldReturn503_whenPythonServiceFails() throws Exception {
        when(restTemplate.getForEntity(anyString(), eq(Object.class)))
                .thenThrow(new RestClientException("down"));

        mockMvc.perform(get("/interview/specialties"))
                .andExpect(status().isServiceUnavailable());
    }

    // ── /interview/cache/{specialtyId} ─────────────────────────────────────

    @Test
    void clearCache_shouldReturnPythonResponse_whenAvailable() throws Exception {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.DELETE), eq(null), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("message", "cache cleared")));

        mockMvc.perform(delete("/interview/cache/java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("cache cleared"));
    }

    @Test
    void clearCache_shouldReturnFallbackMessage_whenPythonServiceFails() throws Exception {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.DELETE), eq(null), eq(Map.class)))
                .thenThrow(new RestClientException("down"));

        mockMvc.perform(delete("/interview/cache/java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cache endpoint non disponible"));
    }
}
