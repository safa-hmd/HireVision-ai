package com.projet.hirevisionai.ServiceImpl;

import com.projet.hirevisionai.Dto.MatchingResultDTO;
import com.projet.hirevisionai.Entity.CV;
import com.projet.hirevisionai.Entity.MatchingResult;
import com.projet.hirevisionai.Repository.CvRepository;
import com.projet.hirevisionai.Repository.MatchingResultRepository;
import com.projet.hirevisionai.ServiceInterface.IMatchingResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchingResultServiceImpl implements IMatchingResultService {

    private final MatchingResultRepository matchingResultRepository;
    private final CvRepository             cvRepository;

    @Override
    public MatchingResultDTO create(MatchingResultDTO dto) {
        CV cv = cvRepository.findById(dto.getCvId())
                .orElseThrow(() -> new RuntimeException("CV introuvable : " + dto.getCvId()));

        MatchingResult saved = matchingResultRepository.save(
                MatchingResult.builder()
                        .score(dto.getScore())
                        .cv(cv)
                        .build());

        return MatchingResultDTO.fromEntity(saved);
    }

    @Override
    public MatchingResultDTO getById(Long id) {
        return MatchingResultDTO.fromEntity(
                matchingResultRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("MatchingResult introuvable : " + id)));
    }

    @Override
    public List<MatchingResultDTO> getByCvId(Long cvId) {
        return matchingResultRepository.findByCvId(cvId)
                .stream().map(MatchingResultDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<MatchingResultDTO> getByUserId(Long userId) {
        return matchingResultRepository.findByCvUserIdUser(userId)
                .stream().map(MatchingResultDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public MatchingResultDTO getBestByCvId(Long cvId) {
        return MatchingResultDTO.fromEntity(
                matchingResultRepository.findTopByCvIdOrderByScoreDesc(cvId)
                        .orElseThrow(() -> new RuntimeException("Aucun résultat pour le CV : " + cvId)));
    }
}
