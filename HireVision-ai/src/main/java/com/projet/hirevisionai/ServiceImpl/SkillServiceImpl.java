package com.projet.hirevisionai.ServiceImpl;

import com.projet.hirevisionai.Dto.SkillDTO;
import com.projet.hirevisionai.Entity.Skill;
import com.projet.hirevisionai.Repository.SkillRepository;
import com.projet.hirevisionai.ServiceInterface.ISkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SkillServiceImpl implements ISkillService {

    private final SkillRepository skillRepository;

    @Override
    public SkillDTO create(SkillDTO dto) {
        if (skillRepository.existsByName(dto.getName()))
            throw new RuntimeException("Skill déjà existant : " + dto.getName());

        return SkillDTO.fromEntity(skillRepository.save(
                Skill.builder()
                        .name(dto.getName())
                        .category(dto.getCategory())
                        .build()));
    }

    @Override
    public SkillDTO getById(Long id) {
        return SkillDTO.fromEntity(
                skillRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Skill introuvable : " + id)));
    }

    @Override
    public List<SkillDTO> getAll() {
        return skillRepository.findAll()
                .stream().map(SkillDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<SkillDTO> getByCategory(String category) {
        return skillRepository.findByCategory(category)
                .stream().map(SkillDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<SkillDTO> search(String keyword) {
        return skillRepository.findByNameContainingIgnoreCase(keyword)
                .stream().map(SkillDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public SkillDTO update(Long id, SkillDTO dto) {
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Skill introuvable : " + id));
        skill.setName(dto.getName());
        skill.setCategory(dto.getCategory());
        return SkillDTO.fromEntity(skillRepository.save(skill));
    }

    @Override
    public void delete(Long id) {
        if (!skillRepository.existsById(id))
            throw new RuntimeException("Skill introuvable : " + id);
        skillRepository.deleteById(id);
    }
}
