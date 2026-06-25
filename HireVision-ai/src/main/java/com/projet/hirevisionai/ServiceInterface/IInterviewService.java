package com.projet.hirevisionai.ServiceInterface;

import com.projet.hirevisionai.Dto.InterviewCreateRequestDTO;
import com.projet.hirevisionai.Dto.InterviewDTO;

import java.time.LocalDateTime;
import java.util.List;

public interface IInterviewService {
    InterviewDTO create(InterviewCreateRequestDTO request);
    InterviewDTO getById(Long id);
    List<InterviewDTO> getByUserId(Long userId);
    List<InterviewDTO> getByUserIdSortedByDate(Long userId);
    List<InterviewDTO> getByDateRange(Long userId,
                                      LocalDateTime start,
                                      LocalDateTime end);
    long countByUserId(Long userId);
    void delete(Long id);
}