package com.projet.hirevisionai.ServiceImpl;

import com.projet.hirevisionai.Dto.AnswerDTO;
import com.projet.hirevisionai.Dto.AnswerSubmitRequestDTO;
import com.projet.hirevisionai.Entity.Answer;
import com.projet.hirevisionai.Entity.Question;
import com.projet.hirevisionai.Repository.AnswerRepository;
import com.projet.hirevisionai.Repository.QuestionRepository;
import com.projet.hirevisionai.ServiceInterface.IAnswerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnswerServiceImpl implements IAnswerService {

    private final AnswerRepository   answerRepository;
    private final QuestionRepository questionRepository;

    @Override
    public AnswerDTO submit(AnswerSubmitRequestDTO request) {
        if (answerRepository.existsByQuestionId(request.getQuestionId()))
            throw new RuntimeException("Question déjà répondue : " + request.getQuestionId());

        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> new RuntimeException("Question introuvable : " + request.getQuestionId()));

        Answer saved = answerRepository.save(
                Answer.builder()
                        .answerText(request.getAnswerText())
                        .score(0f)
                        .aiComment("")
                        .question(question)
                        .build());

        return AnswerDTO.fromEntity(saved);
    }

    @Override
    public AnswerDTO getByQuestionId(Long questionId) {
        return answerRepository.findByQuestionId(questionId)
                .map(AnswerDTO::fromEntity)
                .orElseThrow(() -> new RuntimeException("Aucune réponse pour la question : " + questionId));
    }

    @Override
    public List<AnswerDTO> getByInterviewId(Long interviewId) {
        return answerRepository.findByQuestionInterviewId(interviewId)
                .stream().map(AnswerDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public Double getAverageScoreByInterviewId(Long interviewId) {
        return answerRepository.findAverageScoreByInterviewId(interviewId);
    }

    @Override
    public Double getAverageScoreByUserId(Long userId) {
        return answerRepository.findAverageScoreByUserId(userId);
    }
}
