package com.projet.hirevisionai.Integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projet.hirevisionai.Dto.LoginRequest;
import com.projet.hirevisionai.Dto.RegisterRequest;
import com.projet.hirevisionai.Entity.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Base commune des tests d'intégration "bout en bout" : contexte Spring Boot
 * complet (contrôleurs → sécurité → services → JPA), sur une base H2 en
 * mémoire (profil "test", voir application-test.properties). Aucune couche
 * n'est mockée ici, uniquement les services externes réels (microservice
 * Python d'IA, SMTP) qui sont mockés au cas par cas dans les sous-classes
 * qui en ont besoin.
 *
 * @Transactional : chaque test s'exécute dans une transaction annulée
 * (rollback) à la fin, pour repartir d'une base propre sans avoir à nettoyer
 * manuellement entre les tests (aucun effet de bord, même en cas d'échec).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class IntegrationTestBase {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    /**
     * Enregistre un nouvel utilisateur puis se connecte, et renvoie le JWT
     * obtenu — exactement le parcours qu'un vrai client ferait, en passant
     * par les vrais endpoints /auth/register et /auth/login (BCrypt +
     * AuthenticationManager + JwtService réels).
     */
    protected String registerAndLogin(String fullName, String email, String password, Role role) throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(fullName, email, password, 25, role);
        mockMvc.perform(post("/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andReturn();

        LoginRequest loginRequest = new LoginRequest(email, password);
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn();

        JsonNode body = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        return body.get("token").asText();
    }

    /** Extrait l'idUser depuis la réponse de /auth/login, utile pour construire les URLs suivantes. */
    protected Long extractUserId(String email, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest(email, password);
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn();
        JsonNode body = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        return body.get("idUser").asLong();
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }
}
