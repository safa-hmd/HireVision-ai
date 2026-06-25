package com.projet.hirevisionai.ServiceImpl;

import com.projet.hirevisionai.Dto.MissedSkillDTO;
import com.projet.hirevisionai.Entity.MatchingResult;
import com.projet.hirevisionai.Entity.MissedSkill;
import com.projet.hirevisionai.Repository.MatchingResultRepository;
import com.projet.hirevisionai.Repository.MissedSkillRepository;
import com.projet.hirevisionai.ServiceInterface.IMissedSkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MissedSkillServiceImpl implements IMissedSkillService {

    private final MissedSkillRepository    missedSkillRepository;
    private final MatchingResultRepository matchingResultRepository;

    @Override
    public MissedSkillDTO create(MissedSkillDTO dto) {
        MatchingResult matchingResult = matchingResultRepository.findById(dto.getMatchingResultId())
                .orElseThrow(() -> new RuntimeException("MatchingResult introuvable : " + dto.getMatchingResultId()));

        MissedSkill saved = missedSkillRepository.save(
                MissedSkill.builder()
                        .skillName(dto.getSkillName())
                        .matchingResult(matchingResult)
                        .build());

        return MissedSkillDTO.fromEntity(saved);
    }

    @Override
    public List<MissedSkillDTO> getByMatchingResultId(Long matchingResultId) {
        return missedSkillRepository.findByMatchingResultId(matchingResultId)
                .stream().map(MissedSkillDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(Long id) {
        if (!missedSkillRepository.existsById(id))
            throw new RuntimeException("MissedSkill introuvable : " + id);
        missedSkillRepository.deleteById(id);
    }
}
