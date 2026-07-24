package com.projet.hirevisionai.Controller;

import com.projet.hirevisionai.Entity.Feedback;
import com.projet.hirevisionai.Entity.Interview;
import com.projet.hirevisionai.Repository.FeedbackRepository;
import com.projet.hirevisionai.Repository.InterviewRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/feedbacks")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackRepository feedbackRepository;
    private final InterviewRepository interviewRepository;

    @GetMapping("/user/{userId}/avg-technical")
    public ResponseEntity<Double> getAvgTechnical(@PathVariable Long userId) {
        Double avg = feedbackRepository.findAvgTechnicalScoreByUserId(userId);
        return ResponseEntity.ok(avg != null ? avg : 0.0);
    }

    @GetMapping("/user/{userId}/avg-communication")
    public ResponseEntity<Double> getAvgCommunication(@PathVariable Long userId) {
        Double avg = feedbackRepository.findAvgCommunicationScoreByUserId(userId);
        return ResponseEntity.ok(avg != null ? avg : 0.0);
    }

    @GetMapping("/user/{userId}/avg-confidence")
    public ResponseEntity<Double> getAvgConfidence(@PathVariable Long userId) {
        Double avg = feedbackRepository.findAvgConfidenceScoreByUserId(userId);
        return ResponseEntity.ok(avg != null ? avg : 0.0);
    }

    @PostMapping("/add")
    public ResponseEntity<Feedback> addFeedback(@RequestBody FeedbackRequest request) {
        Interview interview = interviewRepository.findById(request.getInterviewId())
                .orElseThrow(() -> new RuntimeException("Interview not found with id: " + request.getInterviewId()));

        Feedback feedback = Feedback.builder()
                .interview(interview)
                .technicalScore(request.getTechnicalScore())
                .communicationScore(request.getCommunicationScore())
                .confidenceScore(request.getConfidenceScore())
                .eyeContactScore(request.getEyeContactScore())
                .build();

        return ResponseEntity.ok(feedbackRepository.save(feedback));
    }

    @Data
    public static class FeedbackRequest {
        private Long interviewId;
        private float technicalScore;
        private float communicationScore;
        private float confidenceScore;
        private float eyeContactScore;
    }
}