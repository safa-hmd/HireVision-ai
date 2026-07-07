package com.projet.hirevisionai.Controller;

import com.projet.hirevisionai.Dto.DashboardStatsDTO;
import com.projet.hirevisionai.ServiceInterface.IAdminStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {

    private final IAdminStatsService adminStatsService;

    @GetMapping("/overview")
    public ResponseEntity<DashboardStatsDTO> getOverview() {
        return ResponseEntity.ok(adminStatsService.getOverview());
    }
}
