package com.projet.hirevisionai.Controller;

import com.projet.hirevisionai.Dto.QuestionDTO;
import com.projet.hirevisionai.Entity.Question;
import com.projet.hirevisionai.ServiceInterface.IQuestionService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.projet.hirevisionai.Entity.Interview;
import com.projet.hirevisionai.Repository.InterviewRepository;
import com.projet.hirevisionai.Repository.QuestionRepository;
import lombok.Data;

import java.util.List;

@RestController
@RequestMapping("/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionRepository questionRepository;
    private final InterviewRepository interviewRepository;
    private final IQuestionService questionService;

    @PostMapping("/add")
    public ResponseEntity<QuestionDTO> addQuestion(@RequestBody QuestionRequest request) {
        Interview interview = interviewRepository.findById(request.getInterviewId())
                .orElseThrow(() -> new RuntimeException("Interview not found with id: " + request.getInterviewId()));

        Question question = Question.builder()
                .content(request.getContent())
                .difficulty(Question.Difficulty.valueOf(request.getDifficulty().toUpperCase()))
                .interview(interview)
                .build();

        Question saved = questionRepository.save(question);
        return ResponseEntity.ok(QuestionDTO.fromEntity(saved));
    }

    @Data
    public static class QuestionRequest {
        private String content;
        private String difficulty;
        private Long interviewId;
    }
    /** Toutes les questions posées, utilisé par la vue admin "Questions" */
    @GetMapping
    public ResponseEntity<List<QuestionDTO>> getAll() {
        return ResponseEntity.ok(questionService.getAll());
    }

    @GetMapping("/difficulty/{difficulty}")
    public ResponseEntity<List<QuestionDTO>> getByDifficulty(@PathVariable String difficulty) {
        return ResponseEntity.ok(questionService.getByDifficulty(
                Question.Difficulty.valueOf(difficulty.toUpperCase())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        questionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}