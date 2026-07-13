package com.projet.hirevisionai.Controller;

import com.projet.hirevisionai.Dto.AnalyticsDTO;
import com.projet.hirevisionai.ServiceInterface.IAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final IAnalyticsService analyticsService;

    @GetMapping("/overview")
    public ResponseEntity<AnalyticsDTO> getOverview() {
        return ResponseEntity.ok(analyticsService.getOverview());
    }
}
