package com.projet.hirevisionai.ServiceImpl;

import com.projet.hirevisionai.Dto.FeedbackDTO;
import com.projet.hirevisionai.Entity.Feedback;
import com.projet.hirevisionai.Entity.Interview;
import com.projet.hirevisionai.Repository.FeedbackRepository;
import com.projet.hirevisionai.Repository.InterviewRepository;
import com.projet.hirevisionai.ServiceInterface.IFeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements IFeedbackService {

    private final FeedbackRepository  feedbackRepository;
    private final InterviewRepository interviewRepository;

    @Override
    public FeedbackDTO create(FeedbackDTO dto) {
        Interview interview = interviewRepository.findById(dto.getInterviewId())
                .orElseThrow(() -> new RuntimeException("Interview introuvable : " + dto.getInterviewId()));

        Feedback saved = feedbackRepository.save(
                Feedback.builder()
                        .technicalScore(dto.getTechnicalScore())
                        .communicationScore(dto.getCommunicationScore())
                        .confidenceScore(dto.getConfidenceScore())
                        .eyeContactScore(dto.getEyeContactScore())
                        .interview(interview)
                        .build());

        return FeedbackDTO.fromEntity(saved);
    }

    @Override
    public FeedbackDTO getByInterviewId(Long interviewId) {
        return feedbackRepository.findByInterviewId(interviewId)
                .map(FeedbackDTO::fromEntity)
                .orElseThrow(() -> new RuntimeException("Aucun feedback pour l'interview : " + interviewId));
    }

    @Override
    public List<FeedbackDTO> getByUserId(Long userId) {
        return feedbackRepository.findByInterviewUserIdUser(userId)
                .stream().map(FeedbackDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public Double getAvgTechnicalScore(Long userId) {
        return feedbackRepository.findAvgTechnicalScoreByUserId(userId);
    }

    @Override
    public Double getAvgCommunicationScore(Long userId) {
        return feedbackRepository.findAvgCommunicationScoreByUserId(userId);
    }

    @Override
    public Double getAvgConfidenceScore(Long userId) {
        return feedbackRepository.findAvgConfidenceScoreByUserId(userId);
    }
}
