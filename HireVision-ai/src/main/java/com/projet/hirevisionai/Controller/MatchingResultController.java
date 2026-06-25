package com.projet.hirevisionai.Controller;

import com.projet.hirevisionai.Dto.MatchingResultDTO;
import com.projet.hirevisionai.ServiceInterface.IMatchingResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/matching-results")
@RequiredArgsConstructor
public class MatchingResultController {

    private final IMatchingResultService matchingResultService;

    @PostMapping("add")
    public ResponseEntity<MatchingResultDTO> create(@RequestBody MatchingResultDTO dto) {
        return ResponseEntity.ok(matchingResultService.create(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MatchingResultDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(matchingResultService.getById(id));
    }

    @GetMapping("/cv/{cvId}")
    public ResponseEntity<List<MatchingResultDTO>> getByCvId(@PathVariable Long cvId) {
        return ResponseEntity.ok(matchingResultService.getByCvId(cvId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<MatchingResultDTO>> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(matchingResultService.getByUserId(userId));
    }

    @GetMapping("/cv/{cvId}/best")
    public ResponseEntity<MatchingResultDTO> getBestByCvId(@PathVariable Long cvId) {
        return ResponseEntity.ok(matchingResultService.getBestByCvId(cvId));
    }
}
