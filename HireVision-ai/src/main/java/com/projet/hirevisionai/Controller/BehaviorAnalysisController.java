package com.projet.hirevisionai.Controller;

import com.projet.hirevisionai.Entity.BehaviorAnalysis;
import com.projet.hirevisionai.Entity.Interview;
import com.projet.hirevisionai.Repository.BehaviorAnalysisRepository;
import com.projet.hirevisionai.Repository.InterviewRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/behavior-analysis")
@RequiredArgsConstructor
public class BehaviorAnalysisController {

    private final BehaviorAnalysisRepository behaviorAnalysisRepository;
    private final InterviewRepository interviewRepository;

    @GetMapping("/user/{userId}/avg-posture")
    public ResponseEntity<Double> getAvgPosture(@PathVariable Long userId) {
        Double avg = behaviorAnalysisRepository.findAvgPostureScoreByUserId(userId);
        return ResponseEntity.ok(avg != null ? avg : 0.0);
    }

    @GetMapping("/user/{userId}/avg-eye-contact")
    public ResponseEntity<Double> getAvgEyeContact(@PathVariable Long userId) {
        Double avg = behaviorAnalysisRepository.findAvgEyeContactScoreByUserId(userId);
        return ResponseEntity.ok(avg != null ? avg : 0.0);
    }

    @PostMapping("/add")
    public ResponseEntity<BehaviorAnalysis> addBehaviorAnalysis(@RequestBody BehaviorAnalysisRequest request) {
        Interview interview = interviewRepository.findById(request.getInterviewId())
                .orElseThrow(() -> new RuntimeException("Interview not found with id: " + request.getInterviewId()));

        BehaviorAnalysis behaviorAnalysis = BehaviorAnalysis.builder()
                .interview(interview)
                .postureScore(request.getPostureScore())
                .eyeContactScore(request.getEyeContactScore())
                .expressionScore(request.getExpressionScore())
                .videoPath(request.getVideoPath())
                .build();

        return ResponseEntity.ok(behaviorAnalysisRepository.save(behaviorAnalysis));
    }

    @Data
    public static class BehaviorAnalysisRequest {
        private Long interviewId;
        private float postureScore;
        private float eyeContactScore;
        private float expressionScore;
        private String videoPath;
    }
}