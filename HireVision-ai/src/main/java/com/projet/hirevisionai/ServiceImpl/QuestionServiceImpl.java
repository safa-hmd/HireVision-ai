package com.projet.hirevisionai.ServiceImpl;

import com.projet.hirevisionai.Dto.QuestionDTO;
import com.projet.hirevisionai.Entity.Question;
import com.projet.hirevisionai.Repository.QuestionRepository;
import com.projet.hirevisionai.ServiceInterface.IQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements IQuestionService {

    private final QuestionRepository questionRepository;

    @Override
    public List<QuestionDTO> getByDifficulty(Question.Difficulty difficulty) {
        return questionRepository.findByDifficulty(difficulty)
                .stream()
                .map(QuestionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<QuestionDTO> getAll() {
        return questionRepository.findAllByOrderByIdDesc()
                .stream()
                .map(QuestionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(Long id) {
        if (!questionRepository.existsById(id)) {
            throw new RuntimeException("Question introuvable : " + id);
        }
        questionRepository.deleteById(id);
    }
}