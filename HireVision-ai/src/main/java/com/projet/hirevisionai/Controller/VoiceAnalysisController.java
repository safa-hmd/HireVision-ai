package com.projet.hirevisionai.Controller;

import com.projet.hirevisionai.Dto.VoiceAnalysisDTO;
import com.projet.hirevisionai.Dto.VoiceAnalysisResultDTO;
import com.projet.hirevisionai.ServiceInterface.IVoiceAnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/voice-analysis")
@RequiredArgsConstructor
public class VoiceAnalysisController {

    private final IVoiceAnalysisService voiceAnalysisService;

    @PostMapping("/add")
    public ResponseEntity<VoiceAnalysisDTO> save(@Valid @RequestBody VoiceAnalysisResultDTO result) {
        return ResponseEntity.ok(voiceAnalysisService.save(result));
    }

    @GetMapping("/interview/{interviewId}")
    public ResponseEntity<VoiceAnalysisDTO> getByInterviewId(@PathVariable Long interviewId) {
        return ResponseEntity.ok(voiceAnalysisService.getByInterviewId(interviewId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<VoiceAnalysisDTO>> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(voiceAnalysisService.getByUserId(userId));
    }

    @GetMapping("/user/{userId}/avg-clarity")
    public ResponseEntity<Double> getAvgClarity(@PathVariable Long userId) {
        return ResponseEntity.ok(voiceAnalysisService.getAvgClarityScore(userId));
    }

    @GetMapping("/user/{userId}/avg-pace")
    public ResponseEntity<Double> getAvgPace(@PathVariable Long userId) {
        return ResponseEntity.ok(voiceAnalysisService.getAvgPaceScore(userId));
    }
}
