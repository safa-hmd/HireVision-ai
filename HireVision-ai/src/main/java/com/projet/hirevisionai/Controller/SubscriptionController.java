package com.projet.hirevisionai.Controller;

import com.projet.hirevisionai.Dto.PaymentDTO;
import com.projet.hirevisionai.Dto.SubscriptionCreateRequestDTO;
import com.projet.hirevisionai.Dto.SubscriptionDTO;
import com.projet.hirevisionai.Dto.SubscriptionStatsDTO;
import com.projet.hirevisionai.ServiceInterface.ISubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final ISubscriptionService subscriptionService;

    @GetMapping("/admin/overview")
    public ResponseEntity<SubscriptionStatsDTO> getStatsOverview() {
        return ResponseEntity.ok(subscriptionService.getStatsOverview());
    }

    @PostMapping("/subscribe")
    public ResponseEntity<SubscriptionDTO> subscribe(@RequestBody SubscriptionCreateRequestDTO request) {
        return ResponseEntity.ok(subscriptionService.subscribe(request));
    }

    @PostMapping("/cancel/{userId}")
    public ResponseEntity<SubscriptionDTO> cancel(@PathVariable Long userId) {
        return ResponseEntity.ok(subscriptionService.cancel(userId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<SubscriptionDTO>> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(subscriptionService.getByUserId(userId));
    }

    @GetMapping("/user/{userId}/payments")
    public ResponseEntity<List<PaymentDTO>> getPaymentsByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(subscriptionService.getPaymentsByUserId(userId));
    }
}
