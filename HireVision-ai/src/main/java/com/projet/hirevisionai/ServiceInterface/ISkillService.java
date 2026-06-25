package com.projet.hirevisionai.ServiceInterface;

import com.projet.hirevisionai.Dto.SkillDTO;

import java.util.List;

public interface ISkillService {
    SkillDTO create(SkillDTO dto);
    SkillDTO getById(Long id);
    List<SkillDTO> getAll();
    List<SkillDTO> getByCategory(String category);
    List<SkillDTO> search(String keyword);
    SkillDTO update(Long id, SkillDTO dto);
    void delete(Long id);
}