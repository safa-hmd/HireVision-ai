package com.projet.hirevisionai.ServiceImpl;

import com.projet.hirevisionai.Dto.InterviewDTO;
import com.projet.hirevisionai.Dto.RecentInterviewDTO;
import com.projet.hirevisionai.Entity.CV;
import com.projet.hirevisionai.Entity.Interview;
import com.projet.hirevisionai.Entity.Question;
import com.projet.hirevisionai.Entity.User;
import com.projet.hirevisionai.Repository.CvRepository;
import com.projet.hirevisionai.Repository.InterviewRepository;
import com.projet.hirevisionai.Repository.UserRepository;
import com.projet.hirevisionai.ServiceInterface.IInterviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InterviewServiceImpl implements IInterviewService {

    private final InterviewRepository interviewRepository;
    private final UserRepository      userRepository;
    private final CvRepository        cvRepository;

    @Override
    public InterviewDTO getById(Long id) {
        return InterviewDTO.fromEntity(
                interviewRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Interview introuvable : " + id)));
    }

    @Override
    public List<InterviewDTO> getByUserId(Long userId) {
        return interviewRepository.findByUserIdUser(userId)
                .stream().map(InterviewDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<InterviewDTO> getByUserIdSortedByDate(Long userId) {
        return interviewRepository.findByUserIdUserOrderByStartDateDesc(userId)
                .stream().map(InterviewDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<InterviewDTO> getByDateRange(Long userId, LocalDateTime start, LocalDateTime end) {
        return interviewRepository.findByUserIdAndDateRange(userId, start, end)
                .stream().map(InterviewDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public long countByUserId(Long userId) {
        return interviewRepository.countByUserIdUser(userId);
    }

    @Override
    public void delete(Long id) {
        if (!interviewRepository.existsById(id))
            throw new RuntimeException("Interview introuvable : " + id);
        interviewRepository.deleteById(id);
    }

    @Override
    public List<RecentInterviewDTO> getAllForAdmin() {
        return interviewRepository.findAllByOrderByStartDateDesc()
                .stream()
                .map(this::toRecentInterviewDTO)
                .collect(Collectors.toList());
    }

    private RecentInterviewDTO toRecentInterviewDTO(Interview interview) {
        Double globalScore = null;
        if (interview.getFeedback() != null) {
            var f = interview.getFeedback();
            globalScore = (double) ((f.getTechnicalScore() + f.getCommunicationScore()
                    + f.getConfidenceScore() + f.getEyeContactScore()) / 4);
            globalScore = Math.round(globalScore * 10.0) / 10.0;
        }

        String dominantDifficulty = "-";
        if (interview.getQuestions() != null && !interview.getQuestions().isEmpty()) {
            Map<Question.Difficulty, Long> counts = interview.getQuestions().stream()
                    .filter(q -> q.getDifficulty() != null)
                    .collect(Collectors.groupingBy(Question::getDifficulty, Collectors.counting()));
            Optional<Map.Entry<Question.Difficulty, Long>> top = counts.entrySet().stream()
                    .max(Map.Entry.comparingByValue());
            if (top.isPresent()) {
                dominantDifficulty = frenchDifficultyLabel(top.get().getKey());
            }
        }

        return RecentInterviewDTO.builder()
                .id(interview.getId())
                .candidateName(interview.getUser() != null ? interview.getUser().getFullName() : "Inconnu")
                .difficulty(dominantDifficulty)
                .globalScore(globalScore)
                .durationMinutes(interview.getDurationMinutes())
                .startDate(interview.getStartDate())
                .build();
    }

    private String frenchDifficultyLabel(Question.Difficulty difficulty) {
        if (difficulty == null) return "Non défini";
        switch (difficulty) {
            case EASY: return "Facile";
            case MEDIUM: return "Moyen";
            case HARD: return "Difficile";
            default: return difficulty.name();
        }
    }
}
