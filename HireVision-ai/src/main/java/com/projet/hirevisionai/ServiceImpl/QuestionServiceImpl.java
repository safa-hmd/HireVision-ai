package com.projet.hirevisionai.ServiceImpl;

import com.projet.hirevisionai.Dto.QuestionCreateRequestDTO;
import com.projet.hirevisionai.Dto.QuestionDTO;
import com.projet.hirevisionai.Entity.Interview;
import com.projet.hirevisionai.Entity.Question;
import com.projet.hirevisionai.Repository.InterviewRepository;
import com.projet.hirevisionai.Repository.QuestionRepository;
import com.projet.hirevisionai.ServiceInterface.IQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements IQuestionService {

    private final QuestionRepository  questionRepository;
    private final InterviewRepository interviewRepository;

    @Override
    public QuestionDTO create(QuestionCreateRequestDTO request) {
        Interview interview = interviewRepository.findById(request.getInterviewId())
                .orElseThrow(() -> new RuntimeException("Interview introuvable : " + request.getInterviewId()));

        Question saved = questionRepository.save(
                Question.builder()
                        .content(request.getContent())
                        .difficulty(Question.Difficulty.valueOf(request.getDifficulty().toUpperCase()))
                        .interview(interview)
                        .build());

        return QuestionDTO.fromEntity(saved);
    }

    @Override
    public QuestionDTO getById(Long id) {
        return QuestionDTO.fromEntity(
                questionRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Question introuvable : " + id)));
    }

    @Override
    public List<QuestionDTO> getByInterviewId(Long interviewId) {
        return questionRepository.findByInterviewId(interviewId)
                .stream().map(QuestionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<QuestionDTO> getUnanswered(Long interviewId) {
        return questionRepository.findUnansweredByInterviewId(interviewId)
                .stream().map(QuestionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<QuestionDTO> getByDifficulty(Question.Difficulty difficulty) {
        return questionRepository.findByDifficulty(difficulty)
                .stream().map(QuestionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(Long id) {
        if (!questionRepository.existsById(id))
            throw new RuntimeException("Question introuvable : " + id);
        questionRepository.deleteById(id);
    }
}
