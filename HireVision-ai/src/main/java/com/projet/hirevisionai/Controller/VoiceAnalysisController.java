package com.projet.hirevisionai.Controller;

import com.projet.hirevisionai.Entity.Interview;
import com.projet.hirevisionai.Entity.VoiceAnalysis;
import com.projet.hirevisionai.Repository.InterviewRepository;
import com.projet.hirevisionai.Repository.VoiceAnalysisRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/voice-analysis")
@RequiredArgsConstructor
public class VoiceAnalysisController {

    private final VoiceAnalysisRepository voiceAnalysisRepository;
    private final InterviewRepository interviewRepository;

    @GetMapping("/user/{userId}/avg-clarity")
    public ResponseEntity<Double> getAvgClarity(@PathVariable Long userId) {
        Double avg = voiceAnalysisRepository.findAvgClarityScoreByUserId(userId);
        return ResponseEntity.ok(avg != null ? avg : 0.0);
    }

    @PostMapping("/add")
    public ResponseEntity<VoiceAnalysis> addVoiceAnalysis(@RequestBody VoiceAnalysisRequest request) {
        Interview interview = interviewRepository.findById(request.getInterviewId())
                .orElseThrow(() -> new RuntimeException("Interview not found with id: " + request.getInterviewId()));

        VoiceAnalysis voiceAnalysis = VoiceAnalysis.builder()
                .interview(interview)
                .clarityScore(request.getClarityScore())
                .paceScore(request.getPaceScore())
                .tonalVariationScore(request.getTonalVariationScore())
                .audioPath(request.getAudioPath())
                .build();

        return ResponseEntity.ok(voiceAnalysisRepository.save(voiceAnalysis));
    }

    @Data
    public static class VoiceAnalysisRequest {
        private Long interviewId;
        private float clarityScore;
        private float paceScore;
        private float tonalVariationScore;
        private String audioPath;
    }
}