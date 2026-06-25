package com.projet.hirevisionai.ServiceInterface;

import com.projet.hirevisionai.Dto.VoiceAnalysisDTO;
import com.projet.hirevisionai.Dto.VoiceAnalysisResultDTO;

import java.util.List;

public interface IVoiceAnalysisService {
    VoiceAnalysisDTO save(VoiceAnalysisResultDTO result);
    VoiceAnalysisDTO getByInterviewId(Long interviewId);
    List<VoiceAnalysisDTO> getByUserId(Long userId);
    Double getAvgClarityScore(Long userId);
    Double getAvgPaceScore(Long userId);
}
