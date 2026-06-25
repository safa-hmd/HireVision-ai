package com.projet.hirevisionai.ServiceInterface;

import com.projet.hirevisionai.Dto.LearningPlanDTO;

import java.util.List;

public interface ILearningPlanService {
    LearningPlanDTO create(LearningPlanDTO dto);
    List<LearningPlanDTO> getByMissedSkillId(Long missedSkillId);
    List<LearningPlanDTO> getByUserId(Long userId);
    LearningPlanDTO update(Long id, LearningPlanDTO dto);
    void delete(Long id);
}