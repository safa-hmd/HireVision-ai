package com.projet.hirevisionai.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projet.hirevisionai.Dto.PaymentDTO;
import com.projet.hirevisionai.Dto.SubscriptionCreateRequestDTO;
import com.projet.hirevisionai.Dto.SubscriptionDTO;
import com.projet.hirevisionai.Dto.SubscriptionStatsDTO;
import com.projet.hirevisionai.Entity.PlanType;
import com.projet.hirevisionai.ServiceInterface.ISubscriptionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import com.projet.hirevisionai.Security.jwt.JwtAuthFilter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SubscriptionController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class))
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser
class SubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ISubscriptionService subscriptionService;

    @Test
    void getStatsOverview_shouldReturnStats() throws Exception {
        SubscriptionStatsDTO stats = SubscriptionStatsDTO.builder()
                .mrr(1200.0)
                .payingSubscribersCount(40)
                .totalUsers(120)
                .build();
        when(subscriptionService.getStatsOverview()).thenReturn(stats);

        mockMvc.perform(get("/subscriptions/admin/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mrr").value(1200.0))
                .andExpect(jsonPath("$.payingSubscribersCount").value(40));
    }

    @Test
    void subscribe_shouldReturnCreatedSubscription() throws Exception {
        SubscriptionCreateRequestDTO request = SubscriptionCreateRequestDTO.builder()
                .userId(1L).plan(PlanType.PRO).build();
        SubscriptionDTO created = SubscriptionDTO.builder()
                .id(1L).userId(1L).plan("PRO").status("ACTIVE").build();
        when(subscriptionService.subscribe(any(SubscriptionCreateRequestDTO.class))).thenReturn(created);

        mockMvc.perform(post("/subscriptions/subscribe")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plan").value("PRO"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void cancel_shouldReturnCanceledSubscription() throws Exception {
        SubscriptionDTO canceled = SubscriptionDTO.builder()
                .id(1L).userId(1L).plan("PRO").status("CANCELED").build();
        when(subscriptionService.cancel(1L)).thenReturn(canceled);

        mockMvc.perform(post("/subscriptions/cancel/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));
    }

    @Test
    void getByUserId_shouldReturnUserSubscriptions() throws Exception {
        when(subscriptionService.getByUserId(1L)).thenReturn(List.of(
                SubscriptionDTO.builder().id(1L).userId(1L).plan("PRO").status("ACTIVE").build()
        ));

        mockMvc.perform(get("/subscriptions/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getPaymentsByUserId_shouldReturnUserPayments() throws Exception {
        when(subscriptionService.getPaymentsByUserId(1L)).thenReturn(List.of(
                PaymentDTO.builder().id(1L).userId(1L).amount(19.99).status("SUCCESS").build()
        ));

        mockMvc.perform(get("/subscriptions/user/1/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount").value(19.99));
    }
}