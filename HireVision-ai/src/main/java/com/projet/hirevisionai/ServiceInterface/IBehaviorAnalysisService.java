package com.projet.hirevisionai.ServiceInterface;

import com.projet.hirevisionai.Dto.BehaviorAnalysisDTO;
import com.projet.hirevisionai.Dto.BehaviorAnalysisResultDTO;

import java.util.List;

public interface IBehaviorAnalysisService {
    BehaviorAnalysisDTO save(BehaviorAnalysisResultDTO result);
    BehaviorAnalysisDTO getByInterviewId(Long interviewId);
    List<BehaviorAnalysisDTO> getByUserId(Long userId);
    Double getAvgPostureScore(Long userId);
    Double getAvgEyeContactScore(Long userId);
}
