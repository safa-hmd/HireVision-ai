package com.projet.hirevisionai.Controller;

import com.projet.hirevisionai.Dto.InterviewCreateRequestDTO;
import com.projet.hirevisionai.Dto.InterviewDTO;
import com.projet.hirevisionai.Dto.RecentInterviewDTO;
import com.projet.hirevisionai.ServiceInterface.IInterviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final IInterviewService interviewService;

    /** Toutes les interviews, utilisé par la vue admin "Gestion des Entretiens" */
    @GetMapping
    public ResponseEntity<List<RecentInterviewDTO>> getAll() {
        return ResponseEntity.ok(interviewService.getAllForAdmin());
    }

    @PostMapping("add")
    public ResponseEntity<InterviewDTO> create(@Valid @RequestBody InterviewCreateRequestDTO request) {
        return ResponseEntity.ok(interviewService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InterviewDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(interviewService.getById(id));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<InterviewDTO>> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(interviewService.getByUserId(userId));
    }

    @GetMapping("/user/{userId}/sorted")
    public ResponseEntity<List<InterviewDTO>> getSortedByDate(@PathVariable Long userId) {
        return ResponseEntity.ok(interviewService.getByUserIdSortedByDate(userId));
    }

    @GetMapping("/user/{userId}/range")
    public ResponseEntity<List<InterviewDTO>> getByDateRange(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(interviewService.getByDateRange(userId, start, end));
    }

    @GetMapping("/user/{userId}/count")
    public ResponseEntity<Long> countByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(interviewService.countByUserId(userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        interviewService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
