package com.projet.hirevisionai.Security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projet.hirevisionai.Controller.AuthController;
import com.projet.hirevisionai.Controller.PlanController;
import com.projet.hirevisionai.Dto.AuthResponse;
import com.projet.hirevisionai.Dto.LoginRequest;
import com.projet.hirevisionai.Dto.PlanDTO;
import com.projet.hirevisionai.Entity.PlanType;
import com.projet.hirevisionai.Entity.Role;
import com.projet.hirevisionai.Entity.User;
import com.projet.hirevisionai.Repository.UserRepository;
import com.projet.hirevisionai.Security.jwt.JwtAuthFilter;
import com.projet.hirevisionai.Security.jwt.JwtService;
import com.projet.hirevisionai.ServiceImpl.EmailService;
import com.projet.hirevisionai.ServiceInterface.IAuthService;
import com.projet.hirevisionai.ServiceInterface.IPlanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de la chaîne de sécurité réelle (SecurityConfig + JwtAuthFilter),
 * SANS addFilters=false : contrairement aux tests de contrôleurs existants,
 * ceux-ci vérifient que l'authentification/autorisation est effectivement
 * appliquée par Spring Security (401 sans token, accès permis sur /auth/**
 * et en préflight CORS, accès autorisé avec un JWT valide).
 */
@WebMvcTest(controllers = {PlanController.class, AuthController.class})
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtService.class, PasswordConfig.class})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private IPlanService planService;

    @MockitoBean
    private IAuthService authService;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    private User buildUser() {
        return User.builder()
                .idUser(1L).email("adem@test.com").password("encoded")
                .role(Role.CANDIDATE).enabled(true).build();
    }

    @Test
    void protectedEndpoint_shouldReturn401_whenNoAuthorizationHeader() throws Exception {
        mockMvc.perform(get("/plans"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void protectedEndpoint_shouldReturn401_whenTokenIsInvalid() throws Exception {
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("adem@test.com")
                .password("encoded")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_CANDIDATE")))
                .build();
        when(userDetailsService.loadUserByUsername("adem@test.com")).thenReturn(userDetails);

        mockMvc.perform(get("/plans").header("Authorization", "Bearer not.a.valid.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_shouldReturn200_whenTokenIsValid() throws Exception {
        User user = buildUser();
        String token = jwtService.generateToken(user);

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("adem@test.com")
                .password("encoded")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_CANDIDATE")))
                .build();
        when(userDetailsService.loadUserByUsername("adem@test.com")).thenReturn(userDetails);
        when(planService.getAllPlans()).thenReturn(List.of(
                PlanDTO.builder().key(PlanType.PRO).name("Pro").price(19.99).build()
        ));

        mockMvc.perform(get("/plans").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void publicAuthEndpoint_shouldNotReturn401_evenWithoutAuthorizationHeader() throws Exception {
        LoginRequest loginRequest = new LoginRequest("adem@test.com", "password123");
        when(authService.login(any())).thenReturn(
                new AuthResponse("some-token", "adem@test.com", "ROLE_CANDIDATE", 1L)
        );

        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("some-token"));
    }

    @Test
    void preflightOptionsRequest_shouldBePermitted_onProtectedEndpoint() throws Exception {
        mockMvc.perform(options("/plans")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.junit.jupiter.api.Assertions.assertNotEquals(401, status);
                });
    }
}
