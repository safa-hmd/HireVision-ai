package com.projet.hirevisionai.Integration;

import com.projet.hirevisionai.Dto.LoginRequest;
import com.projet.hirevisionai.Dto.RegisterRequest;
import com.projet.hirevisionai.Entity.Role;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test d'intégration bout en bout du parcours d'authentification :
 * inscription → connexion → utilisation du JWT réel pour accéder à un
 * endpoint protégé, sur une vraie base H2 et la vraie chaîne de sécurité
 * (BCrypt, AuthenticationManager, JwtAuthFilter). Rien n'est mocké ici.
 */
class AuthIntegrationTest extends IntegrationTestBase {

    @Test
    void fullAuthFlow_shouldRegisterLoginAndAccessProtectedEndpoint() throws Exception {
        String token = registerAndLogin("Adem Ben", "adem.integration@test.com", "password123", Role.CANDIDATE);
        Long userId = extractUserId("adem.integration@test.com", "password123");

        // Sans token → 401
        mockMvc.perform(get("/users/" + userId))
                .andExpect(status().isUnauthorized());

        // Avec le vrai JWT obtenu au login → 200
        mockMvc.perform(get("/users/" + userId).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("adem.integration@test.com"));
    }

    @Test
    void register_shouldReturn400_whenEmailAlreadyUsed() throws Exception {
        RegisterRequest req = new RegisterRequest("Sami", "sami.integration@test.com", "password123", 30, Role.CANDIDATE);

        mockMvc.perform(post("/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email already used"));
    }

    @Test
    void register_shouldReturn400_whenPasswordTooShort() throws Exception {
        RegisterRequest req = new RegisterRequest("Yassine", "yassine.integration@test.com", "123", 22, Role.CANDIDATE);

        mockMvc.perform(post("/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Password must contain at least 6 characters"));
    }

    @Test
    void login_shouldReturn401_whenPasswordIsWrong() throws Exception {
        RegisterRequest req = new RegisterRequest("Nour", "nour.integration@test.com", "correctPassword", 27, Role.CANDIDATE);
        mockMvc.perform(post("/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        LoginRequest wrongLogin = new LoginRequest("nour.integration@test.com", "wrongPassword");
        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(wrongLogin)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_shouldReturn401_whenUserDoesNotExist() throws Exception {
        LoginRequest login = new LoginRequest("ghost.integration@test.com", "whatever123");

        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_shouldReturn401_whenTokenIsTampered() throws Exception {
        String token = registerAndLogin("Karim", "karim.integration@test.com", "password123", Role.CANDIDATE);
        Long userId = extractUserId("karim.integration@test.com", "password123");

        String tamperedToken = token.substring(0, token.length() - 3) + "xyz";

        mockMvc.perform(get("/users/" + userId).header("Authorization", bearer(tamperedToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void differentUsers_shouldReceiveDifferentTokens() throws Exception {
        String tokenA = registerAndLogin("User A", "usera.integration@test.com", "password123", Role.CANDIDATE);
        String tokenB = registerAndLogin("User B", "userb.integration@test.com", "password123", Role.CANDIDATE);

        org.junit.jupiter.api.Assertions.assertNotEquals(tokenA, tokenB);
    }
}
