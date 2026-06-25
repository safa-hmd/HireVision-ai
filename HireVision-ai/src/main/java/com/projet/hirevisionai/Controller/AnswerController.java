package com.projet.hirevisionai.Controller;

import com.projet.hirevisionai.Dto.AnswerDTO;
import com.projet.hirevisionai.Dto.AnswerSubmitRequestDTO;
import com.projet.hirevisionai.ServiceInterface.IAnswerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/answers")
@RequiredArgsConstructor
public class AnswerController {

    private final IAnswerService answerService;

    @PostMapping("add")
    public ResponseEntity<AnswerDTO> submit(@Valid @RequestBody AnswerSubmitRequestDTO request) {
        return ResponseEntity.ok(answerService.submit(request));
    }

    @GetMapping("/question/{questionId}")
    public ResponseEntity<AnswerDTO> getByQuestionId(@PathVariable Long questionId) {
        return ResponseEntity.ok(answerService.getByQuestionId(questionId));
    }

    @GetMapping("/interview/{interviewId}")
    public ResponseEntity<List<AnswerDTO>> getByInterviewId(@PathVariable Long interviewId) {
        return ResponseEntity.ok(answerService.getByInterviewId(interviewId));
    }

    @GetMapping("/interview/{interviewId}/average-score")
    public ResponseEntity<Double> getAvgScoreByInterview(@PathVariable Long interviewId) {
        return ResponseEntity.ok(answerService.getAverageScoreByInterviewId(interviewId));
    }

    @GetMapping("/user/{userId}/average-score")
    public ResponseEntity<Double> getAvgScoreByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(answerService.getAverageScoreByUserId(userId));
    }
}
