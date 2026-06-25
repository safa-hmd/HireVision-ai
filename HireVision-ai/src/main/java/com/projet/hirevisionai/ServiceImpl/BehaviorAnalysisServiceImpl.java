package com.projet.hirevisionai.ServiceImpl;

import com.projet.hirevisionai.Dto.BehaviorAnalysisDTO;
import com.projet.hirevisionai.Dto.BehaviorAnalysisResultDTO;
import com.projet.hirevisionai.Entity.BehaviorAnalysis;
import com.projet.hirevisionai.Entity.Interview;
import com.projet.hirevisionai.Repository.BehaviorAnalysisRepository;
import com.projet.hirevisionai.Repository.InterviewRepository;
import com.projet.hirevisionai.ServiceInterface.IBehaviorAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BehaviorAnalysisServiceImpl implements IBehaviorAnalysisService {

    private final BehaviorAnalysisRepository behaviorRepo;
    private final InterviewRepository        interviewRepository;

    @Override
    public BehaviorAnalysisDTO save(BehaviorAnalysisResultDTO result) {
        Interview interview = interviewRepository.findById(result.getInterviewId())
                .orElseThrow(() -> new RuntimeException("Interview introuvable : " + result.getInterviewId()));

        BehaviorAnalysis saved = behaviorRepo.save(
                BehaviorAnalysis.builder()
                        .postureScore(result.getPostureScore())
                        .eyeContactScore(result.getEyeContactScore())
                        .expressionScore(result.getExpressionScore())
                        .videoPath(result.getVideoPath())
                        .interview(interview)
                        .build());

        return BehaviorAnalysisDTO.fromEntity(saved);
    }

    @Override
    public BehaviorAnalysisDTO getByInterviewId(Long interviewId) {
        return behaviorRepo.findByInterviewId(interviewId)
                .map(BehaviorAnalysisDTO::fromEntity)
                .orElseThrow(() -> new RuntimeException("Aucune analyse comportementale pour l'interview : " + interviewId));
    }

    @Override
    public List<BehaviorAnalysisDTO> getByUserId(Long userId) {
        return behaviorRepo.findByInterviewUserIdUser(userId)
                .stream().map(BehaviorAnalysisDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public Double getAvgPostureScore(Long userId) {
        return behaviorRepo.findAvgPostureScoreByUserId(userId);
    }

    @Override
    public Double getAvgEyeContactScore(Long userId) {
        return behaviorRepo.findAvgEyeContactScoreByUserId(userId);
    }
}
