package com.projet.hirevisionai.ServiceInterface;

import com.projet.hirevisionai.Dto.JobMatchRequestDTO;
import com.projet.hirevisionai.Dto.MatchingResultDTO;

import java.util.List;

public interface IMatchingResultService {
    List<MatchingResultDTO> getByCvId(Long cvId);
    List<MatchingResultDTO> getByUserId(Long userId);
    MatchingResultDTO getBestByCvId(Long cvId);
    MatchingResultDTO matchAndSave(JobMatchRequestDTO request);
}