package com.projet.hirevisionai.Integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.projet.hirevisionai.Dto.JobMatchRequestDTO;
import com.projet.hirevisionai.Dto.JobOfferDTO;
import com.projet.hirevisionai.Entity.Role;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test d'intégration du parcours métier central de l'application : un
 * candidat s'inscrit, uploade son CV, un job offer est créé, un matching
 * est calculé (le microservice Python de scoring est mocké — c'est un
 * service externe, pas notre code), puis les compétences manquantes
 * persistées sont retrouvées via /missed-skills et /matching-results.
 *
 * Vérifie également, sur ce même scénario réel, que les règles de
 * propriété (checkOwnership dans CvController / MatchingResultController)
 * sont bien appliquées de bout en bout par la vraie chaîne de sécurité :
 * un autre candidat ne peut pas voir les données, un admin le peut.
 */
class CvJobOfferMatchingIntegrationTest extends IntegrationTestBase {

    @MockitoBean
    private RestTemplate restTemplate;

    @Test
    void fullFlow_uploadCv_createJobOffer_runMatching_andPersistMissedSkills() throws Exception {
        String candidateToken = registerAndLogin("Amine Trabelsi", "amine.matching@test.com", "password123", Role.CANDIDATE);
        Long candidateId = extractUserId("amine.matching@test.com", "password123");

        // ── 1. Créer une offre d'emploi ──────────────────────────────
        JobOfferDTO jobOfferInput = JobOfferDTO.builder()
                .title("Développeur Java Spring")
                .company("HireVision")
                .description("Poste de développeur backend")
                .active(true)
                .requiredSkills(List.of("Java", "Spring Boot", "Docker"))
                .build();

        MvcResult jobOfferResult = mockMvc.perform(post("/job-offers")
                        .header("Authorization", bearer(candidateToken))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(jobOfferInput)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Développeur Java Spring"))
                .andReturn();

        JsonNode jobOfferJson = objectMapper.readTree(jobOfferResult.getResponse().getContentAsString());
        Long jobOfferId = jobOfferJson.get("id").asLong();

        // ── 2. Uploader un CV ─────────────────────────────────────────
        MockMultipartFile cvFile = new MockMultipartFile("file", "cv.pdf", "application/pdf", "contenu cv".getBytes());

        MvcResult cvResult = mockMvc.perform(multipart("/cvs/upload")
                        .file(cvFile)
                        .param("userId", candidateId.toString())
                        .header("Authorization", bearer(candidateToken)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode cvJson = objectMapper.readTree(cvResult.getResponse().getContentAsString());
        Long cvId = cvJson.get("id").asLong();

        // ── 3. Calculer le matching (le microservice Python est mocké) ─
        Map<String, Object> pythonResponse = Map.of(
                "score", 72.5,
                "matched", List.of("Java", "Spring Boot"),
                "missing", List.of("Docker"),
                "label", "Bonne compatibilité",
                "message", "Il vous manque une compétence clé",
                "compatible", true
        );
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(org.springframework.http.HttpEntity.class), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(pythonResponse));

        JobMatchRequestDTO matchRequest = new JobMatchRequestDTO();
        matchRequest.setCvId(cvId);
        matchRequest.setCvSkills(List.of("Java", "Spring Boot"));
        matchRequest.setJobOfferId(jobOfferId);

        mockMvc.perform(post("/matching-results/match")
                        .header("Authorization", bearer(candidateToken))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(matchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(72.5))
                .andExpect(jsonPath("$.missingSkills[0]").value("Docker"));

        // ── 4. Vérifier que les compétences manquantes sont bien persistées ─
        mockMvc.perform(get("/missed-skills/user/" + candidateId)
                        .header("Authorization", bearer(candidateToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].skillName").value("Docker"));

        // ── 5. Vérifier le résultat de matching via son propre endpoint ─
        mockMvc.perform(get("/matching-results/user/" + candidateId)
                        .header("Authorization", bearer(candidateToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].jobOfferTitle").value("Développeur Java Spring"));
    }

    @Test
    void otherCandidate_shouldBeForbidden_fromAccessingSomeoneElsesCv() throws Exception {
        String ownerToken = registerAndLogin("Owner", "owner.cv@test.com", "password123", Role.CANDIDATE);
        Long ownerId = extractUserId("owner.cv@test.com", "password123");

        MockMultipartFile cvFile = new MockMultipartFile("file", "cv.pdf", "application/pdf", "contenu".getBytes());
        MvcResult cvResult = mockMvc.perform(multipart("/cvs/upload")
                        .file(cvFile)
                        .param("userId", ownerId.toString())
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andReturn();
        Long cvId = objectMapper.readTree(cvResult.getResponse().getContentAsString()).get("id").asLong();

        String intruderToken = registerAndLogin("Intruder", "intruder.cv@test.com", "password123", Role.CANDIDATE);

        mockMvc.perform(get("/cvs/" + cvId)
                        .header("Authorization", bearer(intruderToken)))
                .andExpect(status().isForbidden());

        // Le propriétaire, lui, peut toujours accéder à son propre CV
        mockMvc.perform(get("/cvs/" + cvId)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk());
    }

    @Test
    void admin_shouldBeAllowed_toAccessAnyCandidatesCv() throws Exception {
        String candidateToken = registerAndLogin("Candidate X", "candidatex.cv@test.com", "password123", Role.CANDIDATE);
        Long candidateId = extractUserId("candidatex.cv@test.com", "password123");

        MockMultipartFile cvFile = new MockMultipartFile("file", "cv.pdf", "application/pdf", "contenu".getBytes());
        MvcResult cvResult = mockMvc.perform(multipart("/cvs/upload")
                        .file(cvFile)
                        .param("userId", candidateId.toString())
                        .header("Authorization", bearer(candidateToken)))
                .andExpect(status().isOk())
                .andReturn();
        Long cvId = objectMapper.readTree(cvResult.getResponse().getContentAsString()).get("id").asLong();

        String adminToken = registerAndLogin("Admin Root", "admin.cv@test.com", "adminPassword123", Role.ADMIN);

        mockMvc.perform(get("/cvs/" + cvId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(candidateId));
    }

    @Test
    void uploadCv_shouldBeForbidden_whenUploadingOnBehalfOfSomeoneElse() throws Exception {
        String userAToken = registerAndLogin("User A", "usera.upload@test.com", "password123", Role.CANDIDATE);
        Long userAId = extractUserId("usera.upload@test.com", "password123");

        registerAndLogin("User B", "userb.upload@test.com", "password123", Role.CANDIDATE);
        Long userBId = extractUserId("userb.upload@test.com", "password123");

        MockMultipartFile cvFile = new MockMultipartFile("file", "cv.pdf", "application/pdf", "contenu".getBytes());

        // User A connecté, mais tente d'uploader "pour" User B → refusé
        mockMvc.perform(multipart("/cvs/upload")
                        .file(cvFile)
                        .param("userId", userBId.toString())
                        .header("Authorization", bearer(userAToken)))
                .andExpect(status().isForbidden());
    }
}
