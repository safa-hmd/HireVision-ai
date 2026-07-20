package com.projet.hirevisionai.ServiceInterface;

import com.projet.hirevisionai.Dto.QuestionDTO;
import com.projet.hirevisionai.Entity.Question;

import java.util.List;

public interface IQuestionService {
    List<QuestionDTO> getByDifficulty(Question.Difficulty difficulty);
    List<QuestionDTO> getAll();
    void delete(Long id);
}
