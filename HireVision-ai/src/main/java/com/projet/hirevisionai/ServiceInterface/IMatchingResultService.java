package com.projet.hirevisionai.ServiceInterface;

import com.projet.hirevisionai.Dto.MatchingResultDTO;

import java.util.List;

public interface IMatchingResultService {
    MatchingResultDTO create(MatchingResultDTO dto);
    MatchingResultDTO getById(Long id);
    List<MatchingResultDTO> getByCvId(Long cvId);
    List<MatchingResultDTO> getByUserId(Long userId);
    MatchingResultDTO getBestByCvId(Long cvId);
}