package com.projet.hirevisionai.ServiceImpl;

import com.projet.hirevisionai.Dto.LearningPlanDTO;
import com.projet.hirevisionai.Entity.Feedback;
import com.projet.hirevisionai.Entity.Interview;
import com.projet.hirevisionai.Entity.LearningPlan;
import com.projet.hirevisionai.Entity.PlanSource;
import com.projet.hirevisionai.Repository.InterviewRepository;
import com.projet.hirevisionai.Repository.LearningPlanRepository;
import com.projet.hirevisionai.ServiceInterface.ILearningPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LearningPlanServiceImpl implements ILearningPlanService {

    private static final float WEAK_THRESHOLD = 60f;

    private final LearningPlanRepository learningPlanRepository;
    private final InterviewRepository interviewRepository;

    @Override
    public List<LearningPlanDTO> getByUserId(Long userId) {
        List<LearningPlan> combined = new ArrayList<>();
        combined.addAll(learningPlanRepository.findByMissedSkillMatchingResultCvUserIdUser(userId));
        combined.addAll(learningPlanRepository.findByInterviewUserIdUser(userId));

        return combined.stream()
                .map(LearningPlanDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<LearningPlanDTO> generateFromInterview(Long interviewId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new RuntimeException("Interview introuvable: " + interviewId));

        Feedback feedback = interview.getFeedback();
        if (feedback == null) {
            throw new RuntimeException("Aucun feedback disponible pour l'entretien: " + interviewId);
        }

        // On repart de zéro pour cet entretien pour éviter les doublons si on régénère
        learningPlanRepository.deleteByInterviewId(interviewId);

        List<LearningPlan> plans = new ArrayList<>();
        addIfWeak(plans, interview, feedback.getTechnicalScore(),
                "Renforcer les compétences techniques",
                "Revoir les fondamentaux techniques abordés pendant l'entretien.");
        addIfWeak(plans, interview, feedback.getCommunicationScore(),
                "Améliorer la communication",
                "Travailler la clarté et la structure des réponses à l'oral.");
        addIfWeak(plans, interview, feedback.getConfidenceScore(),
                "Renforcer la confiance",
                "S'entraîner avec des simulations d'entretien pour gagner en assurance.");
        addIfWeak(plans, interview, feedback.getEyeContactScore(),
                "Travailler le langage non-verbal",
                "Exercices de contact visuel et de posture pendant les réponses.");

        List<LearningPlan> saved = learningPlanRepository.saveAll(plans);

        return saved.stream()
                .map(LearningPlanDTO::fromEntity)
                .collect(Collectors.toList());
    }

    private void addIfWeak(List<LearningPlan> plans, Interview interview, float score,
                           String title, String content) {
        if (score < WEAK_THRESHOLD) {
            int weekNumber = score < 40 ? 1 : 2;
            plans.add(LearningPlan.builder()
                    .title(title)
                    .content(content)
                    .weekNumber(weekNumber)
                    .source(PlanSource.INTERVIEW)
                    .interview(interview)
                    .build());
        }
    }
}