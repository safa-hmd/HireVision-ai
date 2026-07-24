package com.projet.hirevisionai.Controller;

import com.projet.hirevisionai.Entity.Answer;
import com.projet.hirevisionai.Entity.Question;
import com.projet.hirevisionai.Repository.AnswerRepository;
import com.projet.hirevisionai.Repository.QuestionRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/answers")
@RequiredArgsConstructor
public class AnswerController {

    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;

    @GetMapping("/user/{userId}/average-score")
    public ResponseEntity<Double> getAverageScoreByUser(@PathVariable Long userId) {
        Double avg = answerRepository.findAverageScoreByUserId(userId);
        return ResponseEntity.ok(avg != null ? avg : 0.0);
    }

    @GetMapping("/interview/{interviewId}/average-score")
    public ResponseEntity<Double> getAverageScoreByInterview(@PathVariable Long interviewId) {
        Double avg = answerRepository.findAverageScoreByInterviewId(interviewId);
        return ResponseEntity.ok(avg != null ? avg : 0.0);
    }

    @PostMapping("/add")
    public ResponseEntity<Answer> addAnswer(@RequestBody AnswerRequest request) {
        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new RuntimeException("Question not found with id: " + request.getQuestionId()));

        Answer answer = Answer.builder()
                .question(question)
                .answerText(request.getAnswerText())
                .score(request.getScore())
                .aiComment(request.getAiComment())
                .build();

        return ResponseEntity.ok(answerRepository.save(answer));
    }

    @Data
    public static class AnswerRequest {
        private Long questionId;
        private String answerText;
        private float score;
        private String aiComment;
    }
}