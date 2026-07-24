package com.projet.hirevisionai.ServiceInterface;

import com.projet.hirevisionai.Dto.LearningPlanDTO;

import java.util.List;

public interface ILearningPlanService {
    List<LearningPlanDTO> getByUserId(Long userId);
    List<LearningPlanDTO> generateFromInterview(Long interviewId);
}