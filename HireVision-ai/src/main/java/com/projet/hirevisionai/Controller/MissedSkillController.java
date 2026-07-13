package com.projet.hirevisionai.Controller;

import com.projet.hirevisionai.Dto.MissedSkillDTO;
import com.projet.hirevisionai.ServiceInterface.IMissedSkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/missed-skills")
@RequiredArgsConstructor
public class MissedSkillController {

    private final IMissedSkillService missedSkillService;

    @PostMapping("add")
    public ResponseEntity<MissedSkillDTO> create(@RequestBody MissedSkillDTO dto) {
        return ResponseEntity.ok(missedSkillService.create(dto));
    }

    @GetMapping("/matching-result/{matchingResultId}")
    public ResponseEntity<List<MissedSkillDTO>> getByMatchingResultId(@PathVariable Long matchingResultId) {
        return ResponseEntity.ok(missedSkillService.getByMatchingResultId(matchingResultId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<MissedSkillDTO>> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(missedSkillService.getByUserId(userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        missedSkillService.delete(id);
        return ResponseEntity.noContent().build();
    }
}