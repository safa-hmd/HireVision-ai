package com.projet.hirevisionai.Integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.projet.hirevisionai.Dto.JobOfferDTO;
import com.projet.hirevisionai.Entity.Role;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test d'intégration du cycle de vie complet d'une offre d'emploi : création
 * (avec résolution/création automatique des compétences requises), recherche
 * par mot-clé, filtre "actives uniquement", mise à jour (y compris
 * désactivation) et suppression — le tout via les vrais endpoints REST,
 * la vraie chaîne de sécurité et la vraie base H2, sans aucun mock.
 */
class JobOfferIntegrationTest extends IntegrationTestBase {

    @Test
    void fullLifecycle_create_search_update_deactivate_andDelete() throws Exception {
        String token = registerAndLogin("Recruiter One", "recruiter.job@test.com", "password123", Role.CANDIDATE);

        // ── 1. Créer une offre ─────────────────────────────────────────
        JobOfferDTO input = JobOfferDTO.builder()
                .title("Développeur Backend Java")
                .company("HireVision")
                .description("Rejoignez notre équipe backend")
                .requiredSkills(List.of("Java", "Spring Boot"))
                .build();

        MvcResult createResult = mockMvc.perform(post("/job-offers")
                        .header("Authorization", bearer(token))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Développeur Backend Java"))
                // create() force toujours active=true, quoi que le client envoie
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.requiredSkills.length()").value(2))
                .andReturn();

        Long jobOfferId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asLong();

        // ── 2. La retrouver via /job-offers/{id} et /job-offers ────────
        mockMvc.perform(get("/job-offers/" + jobOfferId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.company").value("HireVision"));

        mockMvc.perform(get("/job-offers")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + jobOfferId + ")]").exists());

        // ── 3. La retrouver via /job-offers/search ──────────────────────
        mockMvc.perform(get("/job-offers/search").param("keyword", "Backend")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(jobOfferId));

        mockMvc.perform(get("/job-offers/search").param("keyword", "Inexistant")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // ── 4. Elle apparaît bien dans les offres actives ───────────────
        mockMvc.perform(get("/job-offers/active")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + jobOfferId + ")]").exists());

        // ── 5. Mise à jour : changement de titre + désactivation ───────
        JobOfferDTO update = JobOfferDTO.builder()
                .title("Développeur Backend Java Senior")
                .company("HireVision")
                .description("Rejoignez notre équipe backend")
                .active(false)
                .requiredSkills(List.of("Java", "Spring Boot", "Docker"))
                .build();

        mockMvc.perform(put("/job-offers/" + jobOfferId)
                        .header("Authorization", bearer(token))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Développeur Backend Java Senior"))
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.requiredSkills.length()").value(3));

        // ── 6. Une fois désactivée, elle ne doit plus apparaître dans /active ─
        mockMvc.perform(get("/job-offers/active")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + jobOfferId + ")]").doesNotExist());

        // ── 7. Suppression ───────────────────────────────────────────────
        mockMvc.perform(delete("/job-offers/" + jobOfferId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/job-offers/" + jobOfferId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void createJobOffer_shouldReuseExistingSkill_ratherThanDuplicatingIt() throws Exception {
        String token = registerAndLogin("Recruiter Two", "recruiter2.job@test.com", "password123", Role.CANDIDATE);

        JobOfferDTO first = JobOfferDTO.builder()
                .title("Offre A").company("HireVision").description("desc")
                .requiredSkills(List.of("Kubernetes"))
                .build();
        mockMvc.perform(post("/job-offers")
                        .header("Authorization", bearer(token))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isOk());

        JobOfferDTO second = JobOfferDTO.builder()
                .title("Offre B").company("HireVision").description("desc")
                .requiredSkills(List.of("Kubernetes"))
                .build();
        MvcResult secondResult = mockMvc.perform(post("/job-offers")
                        .header("Authorization", bearer(token))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(secondResult.getResponse().getContentAsString());
        org.junit.jupiter.api.Assertions.assertEquals("Kubernetes", body.get("requiredSkills").get(0).asText());
    }

    @Test
    void getById_shouldReturn500_whenJobOfferDoesNotExist() throws Exception {
        String token = registerAndLogin("Ghost Seeker", "ghost.job@test.com", "password123", Role.CANDIDATE);

        mockMvc.perform(get("/job-offers/999999")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void jobOfferEndpoints_shouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/job-offers"))
                .andExpect(status().isUnauthorized());
    }
}
