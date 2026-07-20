package com.projet.hirevisionai.Controller;

import com.projet.hirevisionai.Dto.LearningPlanDTO;
import com.projet.hirevisionai.ServiceInterface.ILearningPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/learning-plans")
@RequiredArgsConstructor
public class LearningPlanController {

    private final ILearningPlanService learningPlanService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<LearningPlanDTO>> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(learningPlanService.getByUserId(userId));
    }
}