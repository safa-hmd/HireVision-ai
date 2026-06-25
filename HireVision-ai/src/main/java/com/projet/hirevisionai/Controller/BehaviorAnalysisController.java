package com.projet.hirevisionai.Controller;

import com.projet.hirevisionai.Dto.BehaviorAnalysisDTO;
import com.projet.hirevisionai.Dto.BehaviorAnalysisResultDTO;
import com.projet.hirevisionai.ServiceInterface.IBehaviorAnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("behavior-analysis")
@RequiredArgsConstructor
public class BehaviorAnalysisController {

    private final IBehaviorAnalysisService behaviorAnalysisService;

    @PostMapping("add")
    public ResponseEntity<BehaviorAnalysisDTO> save(@Valid @RequestBody BehaviorAnalysisResultDTO result) {
        return ResponseEntity.ok(behaviorAnalysisService.save(result));
    }

    @GetMapping("/interview/{interviewId}")
    public ResponseEntity<BehaviorAnalysisDTO> getByInterviewId(@PathVariable Long interviewId) {
        return ResponseEntity.ok(behaviorAnalysisService.getByInterviewId(interviewId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<BehaviorAnalysisDTO>> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(behaviorAnalysisService.getByUserId(userId));
    }

    @GetMapping("/user/{userId}/avg-posture")
    public ResponseEntity<Double> getAvgPosture(@PathVariable Long userId) {
        return ResponseEntity.ok(behaviorAnalysisService.getAvgPostureScore(userId));
    }

    @GetMapping("/user/{userId}/avg-eye-contact")
    public ResponseEntity<Double> getAvgEyeContact(@PathVariable Long userId) {
        return ResponseEntity.ok(behaviorAnalysisService.getAvgEyeContactScore(userId));
    }
}
