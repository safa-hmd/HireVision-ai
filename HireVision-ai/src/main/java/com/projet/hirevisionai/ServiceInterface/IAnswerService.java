package com.projet.hirevisionai.ServiceInterface;

import com.projet.hirevisionai.Dto.AnswerDTO;
import com.projet.hirevisionai.Dto.AnswerSubmitRequestDTO;

import java.util.List;

public interface IAnswerService {
    AnswerDTO submit(AnswerSubmitRequestDTO request);
    AnswerDTO getByQuestionId(Long questionId);
    List<AnswerDTO> getByInterviewId(Long interviewId);
    Double getAverageScoreByInterviewId(Long interviewId);
    Double getAverageScoreByUserId(Long userId);
}