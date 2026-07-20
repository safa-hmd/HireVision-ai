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

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<MissedSkillDTO>> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(missedSkillService.getByUserId(userId));
    }
}