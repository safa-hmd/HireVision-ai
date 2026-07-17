package com.projet.hirevisionai.Controller;

import com.projet.hirevisionai.Dto.PlanDTO;
import com.projet.hirevisionai.Entity.PlanType;
import com.projet.hirevisionai.ServiceInterface.IPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("plans")
@RequiredArgsConstructor
public class PlanController {

    private final IPlanService planService;

    @GetMapping
    public ResponseEntity<List<PlanDTO>> getAllPlans() {
        return ResponseEntity.ok(planService.getAllPlans());
    }

    @PutMapping("/{key}")
    public ResponseEntity<PlanDTO> updatePlan(@PathVariable PlanType key, @RequestBody PlanDTO dto) {
        return ResponseEntity.ok(planService.updatePlan(key, dto));
    }
}
