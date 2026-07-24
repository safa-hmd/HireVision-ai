package com.projet.hirevisionai.Controller;

import com.projet.hirevisionai.Dto.InterviewDTO;
import com.projet.hirevisionai.Dto.RecentInterviewDTO;
import com.projet.hirevisionai.ServiceInterface.IInterviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.projet.hirevisionai.Entity.CV;
import com.projet.hirevisionai.Entity.Interview;
import com.projet.hirevisionai.Entity.User;
import com.projet.hirevisionai.Repository.CvRepository;
import com.projet.hirevisionai.Repository.InterviewRepository;
import com.projet.hirevisionai.Repository.UserRepository;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final IInterviewService interviewService;
    private final InterviewRepository interviewRepository;
    private final UserRepository userRepository;
    private final CvRepository cvRepository;

    @PostMapping("/add")
    public ResponseEntity<InterviewDTO> addInterview(@RequestBody InterviewRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with id: " + request.getUserId()));

        CV cv = cvRepository.findById(request.getCvId())
                .orElseThrow(() -> new RuntimeException("CV not found with id: " + request.getCvId()));

        Interview interview = Interview.builder()
                .user(user)
                .cv(cv)
                .startDate(request.getStartDate() != null ? request.getStartDate() : LocalDateTime.now())
                .durationMinutes(request.getDurationMinutes())
                .build();

        Interview saved = interviewRepository.save(interview);
        return ResponseEntity.ok(InterviewDTO.fromEntity(saved));
    }

    @Data
    public static class InterviewRequest {
        private Long userId;
        private Long cvId;
        private LocalDateTime startDate;
        private int durationMinutes;
    }

    /** Toutes les interviews, utilisé par la vue admin "Gestion des Entretiens" */
    @GetMapping
    public ResponseEntity<List<RecentInterviewDTO>> getAll() {
        return ResponseEntity.ok(interviewService.getAllForAdmin());
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