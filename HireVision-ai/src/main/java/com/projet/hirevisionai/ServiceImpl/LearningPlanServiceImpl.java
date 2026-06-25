package com.projet.hirevisionai.ServiceImpl;

import com.projet.hirevisionai.Dto.LearningPlanDTO;
import com.projet.hirevisionai.Entity.LearningPlan;
import com.projet.hirevisionai.Entity.MissedSkill;
import com.projet.hirevisionai.Repository.LearningPlanRepository;
import com.projet.hirevisionai.Repository.MissedSkillRepository;
import com.projet.hirevisionai.ServiceInterface.ILearningPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LearningPlanServiceImpl implements ILearningPlanService {

    private final LearningPlanRepository learningPlanRepository;
    private final MissedSkillRepository  missedSkillRepository;

    @Override
    public LearningPlanDTO create(LearningPlanDTO dto) {
        MissedSkill missedSkill = missedSkillRepository.findById(dto.getMissedSkillId())
                .orElseThrow(() -> new RuntimeException("MissedSkill introuvable : " + dto.getMissedSkillId()));

        LearningPlan saved = learningPlanRepository.save(
                LearningPlan.builder()
                        .title(dto.getTitle())
                        .content(dto.getContent())
                        .resourceUrl(dto.getResourceUrl())
                        .missedSkill(missedSkill)
                        .build());

        return LearningPlanDTO.fromEntity(saved);
    }

    @Override
    public List<LearningPlanDTO> getByMissedSkillId(Long missedSkillId) {
        return learningPlanRepository.findByMissedSkillId(missedSkillId)
                .stream().map(LearningPlanDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<LearningPlanDTO> getByUserId(Long userId) {
        return learningPlanRepository.findByMissedSkillMatchingResultCvUserIdUser(userId)
                .stream().map(LearningPlanDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public LearningPlanDTO update(Long id, LearningPlanDTO dto) {
        LearningPlan lp = learningPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("LearningPlan introuvable : " + id));
        lp.setTitle(dto.getTitle());
        lp.setContent(dto.getContent());
        lp.setResourceUrl(dto.getResourceUrl());
        return LearningPlanDTO.fromEntity(learningPlanRepository.save(lp));
    }

    @Override
    public void delete(Long id) {
        if (!learningPlanRepository.existsById(id))
            throw new RuntimeException("LearningPlan introuvable : " + id);
        learningPlanRepository.deleteById(id);
    }
}
