package com.projet.hirevisionai.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projet.hirevisionai.Dto.UserDTO;
import com.projet.hirevisionai.ServiceInterface.IUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import com.projet.hirevisionai.Security.jwt.JwtAuthFilter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IUserService userService;

    @Test
    void getById_shouldReturnUser_whenExists() throws Exception {
        when(userService.getById(1L)).thenReturn(
                UserDTO.builder().idUser(1L).fullName("Adem Ben").email("adem@test.com").role("CANDIDATE").build()
        );

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Adem Ben"));
    }

    @Test
    void getById_shouldReturn500_whenUserNotFound() throws Exception {
        when(userService.getById(99L)).thenThrow(new RuntimeException("User introuvable : 99"));

        mockMvc.perform(get("/users/99"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getAll_shouldReturnAllUsers() throws Exception {
        when(userService.getAll()).thenReturn(List.of(
                UserDTO.builder().idUser(1L).fullName("Adem Ben").build(),
                UserDTO.builder().idUser(2L).fullName("Sara K").build()
        ));

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void update_shouldReturnUpdatedUser_whenExists() throws Exception {
        UserDTO input = UserDTO.builder().fullName("Adem Ben Updated").build();
        UserDTO updated = UserDTO.builder().idUser(1L).fullName("Adem Ben Updated").build();
        when(userService.update(eq(1L), any(UserDTO.class))).thenReturn(updated);

        mockMvc.perform(put("/users/1")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Adem Ben Updated"));
    }

    @Test
    void delete_shouldReturnNoContent_whenUserExists() throws Exception {
        doNothing().when(userService).delete(1L);

        mockMvc.perform(delete("/users/1"))
                .andExpect(status().isNoContent());

        verify(userService).delete(1L);
    }

    @Test
    void uploadPicture_shouldReturnUpdatedUser_whenFileIsValid() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", "img-bytes".getBytes());
        UserDTO updated = UserDTO.builder().idUser(1L).profilePicture("avatar.png").build();
        when(userService.uploadPicture(eq(1L), any())).thenReturn(updated);

        mockMvc.perform(multipart("/users/1/upload-picture").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profilePicture").value("avatar.png"));
    }

    @Test
    void analyzeGithub_shouldReturnAnalysisResult() throws Exception {
        when(userService.analyzeGithub(1L)).thenReturn(Map.of("stars", 42, "repos", 12));

        mockMvc.perform(get("/users/1/analyze-github"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stars").value(42));
    }

    @Test
    void analyzeGithub_shouldReturnBadRequest_whenGithubNotConfigured() throws Exception {
        when(userService.analyzeGithub(2L))
                .thenThrow(new IllegalArgumentException("Veuillez renseigner votre profil GitHub dans vos paramètres."));

        mockMvc.perform(get("/users/2/analyze-github"))
                .andExpect(status().isBadRequest());
    }
}