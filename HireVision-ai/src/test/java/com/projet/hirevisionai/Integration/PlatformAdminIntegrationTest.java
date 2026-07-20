package com.projet.hirevisionai.Integration;

import com.projet.hirevisionai.Dto.PlanDTO;
import com.projet.hirevisionai.Dto.SettingsDTO;
import com.projet.hirevisionai.Dto.UserDTO;
import com.projet.hirevisionai.Entity.PlanType;
import com.projet.hirevisionai.Entity.Role;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'intégration des parcours "back-office" de la plateforme :
 * paramètres globaux (settings), plans tarifaires, gestion des comptes
 * utilisateurs et tableaux de bord (stats admin / analytics). Ces
 * endpoints ne posent aucune vérification de rôle applicative (seule
 * l'authentification est requise, voir SecurityConfig) : ce comportement
 * actuel est documenté tel quel par ces tests plutôt que supposé.
 */
class PlatformAdminIntegrationTest extends IntegrationTestBase {

    @Test
    void settings_shouldReturnDefaults_thenPersistUpdate() throws Exception {
        String token = registerAndLogin("Admin Settings", "admin.settings@test.com", "adminPassword123", Role.ADMIN);

        // ── 1. Au premier appel, les valeurs par défaut sont créées et renvoyées ─
        mockMvc.perform(get("/settings").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.platformName").value("HireVision AI"))
                .andExpect(jsonPath("$.freeInterviewsPerMonth").value(5));

        // ── 2. Mise à jour ────────────────────────────────────────────
        SettingsDTO update = SettingsDTO.builder()
                .platformName("HireVision AI Pro")
                .siteUrl("https://hirevision.ai")
                .supportEmail("support@hirevision.ai")
                .timezone("Africa/Tunis (UTC+1)")
                .language("Français")
                .currency("TND (د.ت)")
                .freeInterviewsPerMonth(10)
                .maxInterviewDuration(60)
                .maxCvSizeMb(8)
                .maxCvPerUser(20)
                .notifNewUser(false)
                .notifPaymentReceived(true)
                .notifDailyReport(false)
                .notifPaymentFailed(true)
                .notifUserReport(true)
                .twoFactor(false)
                .socialLogin(true)
                .videoEncryption(true)
                .gdpr(true)
                .sessionDuration("2 heures")
                .maxLoginAttempts(3)
                .dataRetentionDays(180)
                .build();

        mockMvc.perform(put("/settings")
                        .header("Authorization", bearer(token))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.platformName").value("HireVision AI Pro"))
                .andExpect(jsonPath("$.freeInterviewsPerMonth").value(10));

        // ── 3. La mise à jour est bien persistée pour les appels suivants ─
        mockMvc.perform(get("/settings").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.platformName").value("HireVision AI Pro"))
                .andExpect(jsonPath("$.maxLoginAttempts").value(3));
    }

    @Test
    void plans_shouldBeSeededByDefault_thenUpdatable() throws Exception {
        String token = registerAndLogin("Admin Plans", "admin.plans@test.com", "adminPassword123", Role.ADMIN);

        // ── 1. Les plans par défaut (PRO, PREMIUM) sont créés au premier appel ─
        mockMvc.perform(get("/plans").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].key").value("PRO"))
                .andExpect(jsonPath("$[1].key").value("PREMIUM"));

        // ── 2. Mise à jour du plan PRO ──────────────────────────────────
        PlanDTO updatedPro = PlanDTO.builder()
                .key(PlanType.PRO)
                .name("Pro Plus")
                .price(39)
                .tagline("Nouvelle offre pour candidats actifs")
                .highlighted(true)
                .features(List.of("Analyses illimitées", "Support prioritaire"))
                .build();

        mockMvc.perform(put("/plans/PRO")
                        .header("Authorization", bearer(token))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(updatedPro)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Pro Plus"))
                .andExpect(jsonPath("$.price").value(39.0))
                .andExpect(jsonPath("$.highlighted").value(true));

        // ── 3. La liste des plans reflète bien la mise à jour, triée par prix ─
        mockMvc.perform(get("/plans").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].key").value("PRO"))
                .andExpect(jsonPath("$[0].name").value("Pro Plus"))
                .andExpect(jsonPath("$[1].key").value("PREMIUM"));
    }

    @Test
    void userManagement_getAll_update_thenDelete() throws Exception {
        String adminToken = registerAndLogin("Admin Users", "admin.users@test.com", "adminPassword123", Role.ADMIN);

        String candidateToken = registerAndLogin("Managed Candidate", "managed.users@test.com", "password123", Role.CANDIDATE);
        Long candidateId = extractUserId("managed.users@test.com", "password123");

        // ── 1. La liste de tous les utilisateurs contient bien le candidat ─
        mockMvc.perform(get("/users").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.idUser == " + candidateId + ")]").exists());

        // ── 2. Mise à jour du profil du candidat (par l'admin) ──────────
        UserDTO update = UserDTO.builder()
                .fullName("Managed Candidate Updated")
                .email("managed.users@test.com")
                .age(28)
                .phone("+216 12 345 678")
                .title("Développeuse Full Stack")
                .build();

        mockMvc.perform(put("/users/" + candidateId)
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Managed Candidate Updated"))
                .andExpect(jsonPath("$.title").value("Développeuse Full Stack"));

        // ── 3. Suppression du compte ──────────────────────────────────
        mockMvc.perform(delete("/users/" + candidateId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/users/" + candidateId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isInternalServerError());

        // Le token précédemment émis pour ce compte reste structurellement
        // valide (le JWT n'est pas révoqué), mais l'utilisateur sous-jacent
        // n'existe plus : la ressource visée n'est plus accessible.
        org.junit.jupiter.api.Assertions.assertNotNull(candidateToken);
    }

    @Test
    void adminStatsOverview_shouldReflectRegisteredUsers() throws Exception {
        String token = registerAndLogin("Admin Stats", "admin.stats@test.com", "adminPassword123", Role.ADMIN);
        registerAndLogin("Fresh Candidate", "fresh.stats@test.com", "password123", Role.CANDIDATE);

        mockMvc.perform(get("/admin/stats/overview").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(greaterThanOrEqualTo(2)));
    }

    @Test
    void analyticsOverview_shouldReturnEmptyButValidStructure_whenNoDataYet() throws Exception {
        String token = registerAndLogin("Admin Analytics", "admin.analytics@test.com", "adminPassword123", Role.ADMIN);

        mockMvc.perform(get("/admin/analytics/overview").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCvsAnalyzed").value(greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.totalMatchings").value(greaterThanOrEqualTo(0)));
    }

    @Test
    void backOfficeEndpoints_shouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/settings")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/plans")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/users")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/admin/stats/overview")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/admin/analytics/overview")).andExpect(status().isUnauthorized());
    }
}
