package com.projet.hirevisionai.ServiceInterface;

import com.projet.hirevisionai.Dto.MissedSkillDTO;

import java.util.List;

public interface IMissedSkillService {
    MissedSkillDTO create(MissedSkillDTO dto);
    List<MissedSkillDTO> getByMatchingResultId(Long matchingResultId);
    void delete(Long id);
}