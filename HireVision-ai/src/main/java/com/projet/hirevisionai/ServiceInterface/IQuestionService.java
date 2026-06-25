package com.projet.hirevisionai.ServiceInterface;

import com.projet.hirevisionai.Dto.QuestionCreateRequestDTO;
import com.projet.hirevisionai.Dto.QuestionDTO;
import com.projet.hirevisionai.Entity.Question;

import java.util.List;

public interface IQuestionService {
    QuestionDTO create(QuestionCreateRequestDTO request);
    QuestionDTO getById(Long id);
    List<QuestionDTO> getByInterviewId(Long interviewId);
    List<QuestionDTO> getUnanswered(Long interviewId);
    List<QuestionDTO> getByDifficulty(Question.Difficulty difficulty);
    void delete(Long id);
}
