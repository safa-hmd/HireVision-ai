package com.projet.hirevisionai.ServiceInterface;

import com.projet.hirevisionai.Dto.FeedbackDTO;

import java.util.List;

public interface IFeedbackService {
    FeedbackDTO create(FeedbackDTO dto);
    FeedbackDTO getByInterviewId(Long interviewId);
    List<FeedbackDTO> getByUserId(Long userId);
    Double getAvgTechnicalScore(Long userId);
    Double getAvgCommunicationScore(Long userId);
    Double getAvgConfidenceScore(Long userId);
}