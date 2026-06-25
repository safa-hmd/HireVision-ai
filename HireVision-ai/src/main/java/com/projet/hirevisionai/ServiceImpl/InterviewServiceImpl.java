package com.projet.hirevisionai.ServiceImpl;

import com.projet.hirevisionai.Dto.InterviewCreateRequestDTO;
import com.projet.hirevisionai.Dto.InterviewDTO;
import com.projet.hirevisionai.Entity.CV;
import com.projet.hirevisionai.Entity.Interview;
import com.projet.hirevisionai.Entity.User;
import com.projet.hirevisionai.Repository.CvRepository;
import com.projet.hirevisionai.Repository.InterviewRepository;
import com.projet.hirevisionai.Repository.UserRepository;
import com.projet.hirevisionai.ServiceInterface.IInterviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InterviewServiceImpl implements IInterviewService {

    private final InterviewRepository interviewRepository;
    private final UserRepository      userRepository;
    private final CvRepository        cvRepository;

    @Override
    public InterviewDTO create(InterviewCreateRequestDTO request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User introuvable : " + request.getUserId()));
        CV cv = cvRepository.findById(request.getCvId())
                .orElseThrow(() -> new RuntimeException("CV introuvable : " + request.getCvId()));

        Interview saved = interviewRepository.save(
                Interview.builder()
                        .user(user)
                        .cv(cv)
                        .startDate(request.getStartDate() != null ? request.getStartDate() : LocalDateTime.now())
                        .build());

        return InterviewDTO.fromEntity(saved);
    }

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
}
