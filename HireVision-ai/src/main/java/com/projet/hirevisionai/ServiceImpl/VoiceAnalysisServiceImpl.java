package com.projet.hirevisionai.ServiceImpl;

import com.projet.hirevisionai.Dto.VoiceAnalysisDTO;
import com.projet.hirevisionai.Dto.VoiceAnalysisResultDTO;
import com.projet.hirevisionai.Entity.Interview;
import com.projet.hirevisionai.Entity.VoiceAnalysis;
import com.projet.hirevisionai.Repository.InterviewRepository;
import com.projet.hirevisionai.Repository.VoiceAnalysisRepository;
import com.projet.hirevisionai.ServiceInterface.IVoiceAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoiceAnalysisServiceImpl implements IVoiceAnalysisService {

    private final VoiceAnalysisRepository voiceAnalysisRepository;
    private final InterviewRepository     interviewRepository;

    @Override
    public VoiceAnalysisDTO save(VoiceAnalysisResultDTO result) {
        Interview interview = interviewRepository.findById(result.getInterviewId())
                .orElseThrow(() -> new RuntimeException("Interview introuvable : " + result.getInterviewId()));

        VoiceAnalysis saved = voiceAnalysisRepository.save(
                VoiceAnalysis.builder()
                        .clarityScore(result.getClarityScore())
                        .paceScore(result.getPaceScore())
                        .tonalVariationScore(result.getTonalVariationScore())
                        .audioPath(result.getAudioPath())
                        .interview(interview)
                        .build());

        return VoiceAnalysisDTO.fromEntity(saved);
    }

    @Override
    public VoiceAnalysisDTO getByInterviewId(Long interviewId) {
        return voiceAnalysisRepository.findByInterviewId(interviewId)
                .map(VoiceAnalysisDTO::fromEntity)
                .orElseThrow(() -> new RuntimeException("Aucune analyse vocale pour l'interview : " + interviewId));
    }

    @Override
    public List<VoiceAnalysisDTO> getByUserId(Long userId) {
        return voiceAnalysisRepository.findByInterviewUserIdUser(userId)
                .stream().map(VoiceAnalysisDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public Double getAvgClarityScore(Long userId) {
        return voiceAnalysisRepository.findAvgClarityScoreByUserId(userId);
    }

    @Override
    public Double getAvgPaceScore(Long userId) {
        return voiceAnalysisRepository.findAvgPaceScoreByUserId(userId);
    }
}
