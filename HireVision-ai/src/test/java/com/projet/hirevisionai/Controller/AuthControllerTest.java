package com.projet.hirevisionai.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projet.hirevisionai.Dto.*;
import com.projet.hirevisionai.Entity.Role;
import com.projet.hirevisionai.Entity.User;
import com.projet.hirevisionai.Repository.UserRepository;
import com.projet.hirevisionai.ServiceImpl.EmailService;
import com.projet.hirevisionai.ServiceInterface.IAuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import com.projet.hirevisionai.Security.jwt.JwtAuthFilter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests controller pour /auth.
 * excludeFilters exclut JwtAuthFilter du contexte du slice : @WebMvcTest instancie
 * automatiquement les beans Filter présents sur le classpath, et JwtAuthFilter a besoin
 * de JwtService/CustomUserDetailsService qui ne sont pas chargés dans un slice de test.
 * Sans cette exclusion, le contexte ne démarre pas (UnsatisfiedDependencyException).
 */
@WebMvcTest(controllers = AuthController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IAuthService authService;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @WithMockUser
    void register_shouldReturnOk_whenDataIsValid() throws Exception {
        RegisterRequest req = new RegisterRequest("Adem Ben", "adem@test.com", "pass123", 22, Role.CANDIDATE);
        User saved = User.builder().idUser(1L).email("adem@test.com").fullName("Adem Ben").role(Role.CANDIDATE).build();
        when(authService.register(any(RegisterRequest.class))).thenReturn(saved);

        mockMvc.perform(post("/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().string("User created: adem@test.com"));

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    @WithMockUser
    void login_shouldReturnToken_whenCredentialsAreValid() throws Exception {
        LoginRequest req = new LoginRequest("adem@test.com", "pass123");
        AuthResponse response = new AuthResponse("jwt-token", "adem@test.com", "CANDIDATE", 1L);
        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.email").value("adem@test.com"))
                .andExpect(jsonPath("$.role").value("CANDIDATE"));
    }

    @Test
    @WithMockUser
    void login_shouldReturn500_whenCredentialsAreInvalid() throws Exception {
        LoginRequest req = new LoginRequest("adem@test.com", "wrongpass");
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new RuntimeException("Mot de passe invalide"));

        // IAuthServiceImp lève une RuntimeException simple sur échec d'authentification,
        // récupérée par le handler générique -> 500 (voir GlobalExceptionHandler).
        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser
    void completeGoogleRegister_shouldReturnToken_whenDataIsValid() throws Exception {
        CompleteGoogleRegisterRequest req = new CompleteGoogleRegisterRequest("adem@gmail.com", "Adem Ben", Role.CANDIDATE);
        AuthResponse response = new AuthResponse("jwt-token", "adem@gmail.com", "CANDIDATE", 2L);
        when(authService.completeGoogleRegister(any(CompleteGoogleRegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/complete-google-register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUser").value(2));
    }

    @Test
    @WithMockUser
    void forgotPassword_shouldReturnOk_whenEmailExists() throws Exception {
        ForgotPasswordRequest req = new ForgotPasswordRequest("adem@test.com");
        doNothing().when(authService).forgotPassword(any(ForgotPasswordRequest.class));

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().string("Email de réinitialisation envoyé"));
    }

    @Test
    @WithMockUser
    void forgotPassword_shouldReturnBadRequest_whenEmailDoesNotExist() throws Exception {
        ForgotPasswordRequest req = new ForgotPasswordRequest("inconnu@test.com");
        doThrow(new IllegalArgumentException("Aucun compte associé à cet email"))
                .when(authService).forgotPassword(any(ForgotPasswordRequest.class));

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Aucun compte associé à cet email"));
    }

    @Test
    @WithMockUser
    void resetPassword_shouldReturnOk_whenTokenIsValid() throws Exception {
        ResetPasswordRequest req = new ResetPasswordRequest("valid-token", "newPass123");
        doNothing().when(authService).resetPassword(any(ResetPasswordRequest.class));

        mockMvc.perform(post("/auth/reset-password")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().string("Mot de passe réinitialisé avec succès"));
    }

    @Test
    @WithMockUser
    void getUserId_shouldReturnId_whenEmailExists() throws Exception {
        User user = User.builder().idUser(7L).email("adem@test.com").role(Role.CANDIDATE).build();
        when(userRepository.findByEmail("adem@test.com")).thenReturn(Optional.of(user));

        mockMvc.perform(get("/auth/getUserId").param("email", "adem@test.com"))
                .andExpect(status().isOk())
                .andExpect(content().string("7"));
    }

    @Test
    @WithMockUser
    void getUserId_shouldReturn500_whenEmailNotFound() throws Exception {
        when(userRepository.findByEmail("absent@test.com")).thenReturn(Optional.empty());

        mockMvc.perform(get("/auth/getUserId").param("email", "absent@test.com"))
                .andExpect(status().isInternalServerError());
    }
}