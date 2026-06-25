package com.projet.hirevisionai.Controller;

import com.projet.hirevisionai.Dto.FeedbackDTO;
import com.projet.hirevisionai.ServiceInterface.IFeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/feedbacks")
@RequiredArgsConstructor
public class FeedbackController {

    private final IFeedbackService feedbackService;

    @PostMapping("add")
    public ResponseEntity<FeedbackDTO> create(@RequestBody FeedbackDTO dto) {
        return ResponseEntity.ok(feedbackService.create(dto));
    }

    @GetMapping("/interview/{interviewId}")
    public ResponseEntity<FeedbackDTO> getByInterviewId(@PathVariable Long interviewId) {
        return ResponseEntity.ok(feedbackService.getByInterviewId(interviewId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<FeedbackDTO>> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(feedbackService.getByUserId(userId));
    }

    @GetMapping("/user/{userId}/avg-technical")
    public ResponseEntity<Double> getAvgTechnical(@PathVariable Long userId) {
        return ResponseEntity.ok(feedbackService.getAvgTechnicalScore(userId));
    }

    @GetMapping("/user/{userId}/avg-communication")
    public ResponseEntity<Double> getAvgCommunication(@PathVariable Long userId) {
        return ResponseEntity.ok(feedbackService.getAvgCommunicationScore(userId));
    }

    @GetMapping("/user/{userId}/avg-confidence")
    public ResponseEntity<Double> getAvgConfidence(@PathVariable Long userId) {
        return ResponseEntity.ok(feedbackService.getAvgConfidenceScore(userId));
    }
}
