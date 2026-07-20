package com.projet.hirevisionai.ServiceInterface;

import com.projet.hirevisionai.Dto.MissedSkillDTO;

import java.util.List;

public interface IMissedSkillService {
    List<MissedSkillDTO> getByUserId(Long userId);
}