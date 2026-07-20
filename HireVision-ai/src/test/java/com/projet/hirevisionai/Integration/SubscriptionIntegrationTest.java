package com.projet.hirevisionai.Integration;

import com.projet.hirevisionai.Dto.SubscriptionCreateRequestDTO;
import com.projet.hirevisionai.Entity.PlanType;
import com.projet.hirevisionai.Entity.Role;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test d'intégration du cycle de vie d'un abonnement : souscription,
 * génération du paiement associé, annulation, re-souscription à un autre
 * plan. Aucun service externe n'est impliqué ici (pas de vraie passerelle
 * de paiement dans le code), donc tout tourne sur la vraie base H2 sans
 * aucun mock.
 */
class SubscriptionIntegrationTest extends IntegrationTestBase {

    @Test
    void fullSubscriptionLifecycle_subscribe_thenCancel_thenResubscribe() throws Exception {
        String token = registerAndLogin("Sophia Sub", "sophia.sub@test.com", "password123", Role.CANDIDATE);
        Long userId = extractUserId("sophia.sub@test.com", "password123");

        // ── 1. Souscrire au plan PRO ──────────────────────────────────
        SubscriptionCreateRequestDTO subscribeRequest = new SubscriptionCreateRequestDTO(userId, PlanType.PRO);

        mockMvc.perform(post("/subscriptions/subscribe")
                        .header("Authorization", bearer(token))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(subscribeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan").value("PRO"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // ── 2. Vérifier la liste des abonnements et le paiement généré ─
        mockMvc.perform(get("/subscriptions/user/" + userId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));

        mockMvc.perform(get("/subscriptions/user/" + userId + "/payments")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].plan").value("PRO"))
                .andExpect(jsonPath("$[0].status").value("PAID"))
                .andExpect(jsonPath("$[0].amount").value(29.0));

        // ── 3. Annuler l'abonnement ───────────────────────────────────
        mockMvc.perform(post("/subscriptions/cancel/" + userId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));

        // ── 4. Re-souscrire à un plan différent : l'ancien abonnement
        // annulé ne doit pas interférer, un nouvel abonnement ACTIVE est créé ─
        SubscriptionCreateRequestDTO resubscribeRequest = new SubscriptionCreateRequestDTO(userId, PlanType.PREMIUM);

        mockMvc.perform(post("/subscriptions/subscribe")
                        .header("Authorization", bearer(token))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(resubscribeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan").value("PREMIUM"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(get("/subscriptions/user/" + userId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void subscribe_shouldReturn500_whenUserDoesNotExist() throws Exception {
        String token = registerAndLogin("Bystander", "bystander.sub@test.com", "password123", Role.CANDIDATE);

        SubscriptionCreateRequestDTO request = new SubscriptionCreateRequestDTO(999999L, PlanType.PRO);

        // Le service lève une RuntimeException simple (pas de handler dédié) →
        // récupérée par le fallback générique de GlobalExceptionHandler → 500.
        mockMvc.perform(post("/subscriptions/subscribe")
                        .header("Authorization", bearer(token))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void cancel_shouldReturn500_whenNoActiveSubscriptionExists() throws Exception {
        String token = registerAndLogin("NoSub", "nosub.sub@test.com", "password123", Role.CANDIDATE);
        Long userId = extractUserId("nosub.sub@test.com", "password123");

        mockMvc.perform(post("/subscriptions/cancel/" + userId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void statsOverview_shouldReflectPayingSubscribers() throws Exception {
        String token = registerAndLogin("Statistician", "stats.sub@test.com", "password123", Role.CANDIDATE);
        Long userId = extractUserId("stats.sub@test.com", "password123");

        mockMvc.perform(post("/subscriptions/subscribe")
                        .header("Authorization", bearer(token))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new SubscriptionCreateRequestDTO(userId, PlanType.PREMIUM))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/subscriptions/admin/overview")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payingSubscribersCount").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }
}
