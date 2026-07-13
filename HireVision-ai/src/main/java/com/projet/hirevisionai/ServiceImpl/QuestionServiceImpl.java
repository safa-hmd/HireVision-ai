package com.projet.hirevisionai.ServiceImpl;

import com.projet.hirevisionai.Dto.GenerateQuestionsRequestDTO;
import com.projet.hirevisionai.Dto.QuestionCreateRequestDTO;
import com.projet.hirevisionai.Dto.QuestionDTO;
import com.projet.hirevisionai.Entity.Interview;
import com.projet.hirevisionai.Entity.Question;
import com.projet.hirevisionai.Repository.InterviewRepository;
import com.projet.hirevisionai.Repository.QuestionRepository;
import com.projet.hirevisionai.ServiceInterface.IQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements IQuestionService {

    private final QuestionRepository questionRepository;
    private final InterviewRepository interviewRepository;
    private final RestTemplate restTemplate;

    @Override
    public QuestionDTO create(QuestionCreateRequestDTO request) {
        Interview interview = interviewRepository.findById(request.getInterviewId())
                .orElseThrow(() ->
                        new RuntimeException("Interview introuvable : " + request.getInterviewId()));

        Question saved = questionRepository.save(
                Question.builder()
                        .content(request.getContent())
                        .difficulty(Question.Difficulty.valueOf(request.getDifficulty().toUpperCase()))
                        .interview(interview)
                        .build()
        );

        return QuestionDTO.fromEntity(saved);
    }

    @Override
    public QuestionDTO getById(Long id) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Question introuvable : " + id));

        return QuestionDTO.fromEntity(question);
    }

    @Override
    public List<QuestionDTO> getByInterviewId(Long interviewId) {
        return questionRepository.findByInterviewId(interviewId)
                .stream()
                .map(QuestionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<QuestionDTO> getUnanswered(Long interviewId) {
        return questionRepository.findUnansweredByInterviewId(interviewId)
                .stream()
                .map(QuestionDTO::fromEntity)
                .collect(Collectors.toList());
    }

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

    @Override
    public Map<String, Object> generateFromPython(GenerateQuestionsRequestDTO request) {

        Map<String, Object> body = new HashMap<>();
        body.put("skills", request.getSkills());
        body.put("job_description", request.getJobDescription());
        body.put("candidate_name", request.getCandidateName());
        body.put("profile", request.getProfile());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(body, headers);

        try {

            ResponseEntity<Map> response = restTemplate.exchange(
                    "http://localhost:8000/generate-questions",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            return response.getBody();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Erreur lors de la génération des questions : "
                            + e.getMessage()
            );
        }
    }
}