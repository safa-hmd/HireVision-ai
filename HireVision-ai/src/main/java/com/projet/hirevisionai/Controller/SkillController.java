package com.projet.hirevisionai.Controller;

import com.projet.hirevisionai.Dto.SkillDTO;
import com.projet.hirevisionai.ServiceInterface.ISkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("skills")
@RequiredArgsConstructor
public class SkillController {

    private final ISkillService skillService;

    @PostMapping("add")
    public ResponseEntity<SkillDTO> create(@RequestBody SkillDTO dto) {
        return ResponseEntity.ok(skillService.create(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SkillDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(skillService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<SkillDTO>> getAll() {
        return ResponseEntity.ok(skillService.getAll());
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<SkillDTO>> getByCategory(@PathVariable String category) {
        return ResponseEntity.ok(skillService.getByCategory(category));
    }

    @GetMapping("/search")
    public ResponseEntity<List<SkillDTO>> search(@RequestParam String keyword) {
        return ResponseEntity.ok(skillService.search(keyword));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SkillDTO> update(@PathVariable Long id, @RequestBody SkillDTO dto) {
        return ResponseEntity.ok(skillService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        skillService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
