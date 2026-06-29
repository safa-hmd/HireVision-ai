package com.projet.hirevisionai.Controller;

import com.projet.hirevisionai.Dto.GenerateQuestionsRequestDTO;
import com.projet.hirevisionai.Dto.QuestionCreateRequestDTO;
import com.projet.hirevisionai.Dto.QuestionDTO;
import com.projet.hirevisionai.Entity.Question;
import com.projet.hirevisionai.ServiceInterface.IQuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final IQuestionService questionService;

    @PostMapping("add")
    public ResponseEntity<QuestionDTO> create(@Valid @RequestBody QuestionCreateRequestDTO request) {
        return ResponseEntity.ok(questionService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuestionDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(questionService.getById(id));
    }

    @GetMapping("/interview/{interviewId}")
    public ResponseEntity<List<QuestionDTO>> getByInterviewId(@PathVariable Long interviewId) {
        return ResponseEntity.ok(questionService.getByInterviewId(interviewId));
    }

    @GetMapping("/interview/{interviewId}/unanswered")
    public ResponseEntity<List<QuestionDTO>> getUnanswered(@PathVariable Long interviewId) {
        return ResponseEntity.ok(questionService.getUnanswered(interviewId));
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

    @PostMapping("/generate")
    public ResponseEntity<Map> generateQuestions(@RequestBody GenerateQuestionsRequestDTO request) {
        return ResponseEntity.ok(questionService.generateFromPython(request));
    }
}
