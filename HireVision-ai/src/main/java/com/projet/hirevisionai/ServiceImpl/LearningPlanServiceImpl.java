package com.projet.hirevisionai.ServiceImpl;

import com.projet.hirevisionai.Dto.LearningPlanDTO;
import com.projet.hirevisionai.Entity.LearningPlan;
import com.projet.hirevisionai.Repository.LearningPlanRepository;
import com.projet.hirevisionai.ServiceInterface.ILearningPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LearningPlanServiceImpl implements ILearningPlanService {

    private final LearningPlanRepository learningPlanRepository;

    @Override
    public List<LearningPlanDTO> getByUserId(Long userId) {
        List<LearningPlan> combined = new ArrayList<>();
        combined.addAll(learningPlanRepository.findByMissedSkillMatchingResultCvUserIdUser(userId));
        combined.addAll(learningPlanRepository.findByInterviewUserIdUser(userId));

        return combined.stream()
                .map(LearningPlanDTO::fromEntity)
                .collect(Collectors.toList());
    }
}