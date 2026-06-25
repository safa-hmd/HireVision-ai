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

    @PostMapping("add")
    public ResponseEntity<LearningPlanDTO> create(@RequestBody LearningPlanDTO dto) {
        return ResponseEntity.ok(learningPlanService.create(dto));
    }

    @GetMapping("/missed-skill/{missedSkillId}")
    public ResponseEntity<List<LearningPlanDTO>> getByMissedSkillId(@PathVariable Long missedSkillId) {
        return ResponseEntity.ok(learningPlanService.getByMissedSkillId(missedSkillId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<LearningPlanDTO>> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(learningPlanService.getByUserId(userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LearningPlanDTO> update(@PathVariable Long id, @RequestBody LearningPlanDTO dto) {
        return ResponseEntity.ok(learningPlanService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        learningPlanService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
