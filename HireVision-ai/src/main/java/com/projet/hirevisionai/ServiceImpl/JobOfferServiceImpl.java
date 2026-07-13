package com.projet.hirevisionai.ServiceImpl;

import com.projet.hirevisionai.Dto.JobOfferDTO;
import com.projet.hirevisionai.Entity.JobOffer;
import com.projet.hirevisionai.Entity.Skill;
import com.projet.hirevisionai.Repository.JobOfferRepository;
import com.projet.hirevisionai.Repository.SkillRepository;
import com.projet.hirevisionai.ServiceInterface.IJobOfferService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobOfferServiceImpl implements IJobOfferService {

    private final JobOfferRepository jobOfferRepository;
    private final SkillRepository skillRepository;

    /** Résout une liste de noms en entités Skill, en créant celles qui n'existent pas encore. */
    private List<Skill> resolveSkills(List<String> names) {
        List<Skill> skills = new ArrayList<>();
        if (names == null) return skills;
        for (String name : names) {
            if (name == null || name.isBlank()) continue;
            String trimmed = name.trim();
            Skill skill = skillRepository.findByName(trimmed)
                    .orElseGet(() -> skillRepository.save(
                            Skill.builder().name(trimmed).category("Autre").build()));
            skills.add(skill);
        }
        return skills;
    }

    @Override
    public JobOfferDTO create(JobOfferDTO dto) {
        JobOffer offer = JobOffer.builder()
                .title(dto.getTitle())
                .company(dto.getCompany())
                .description(dto.getDescription())
                .active(true)
                .requiredSkills(resolveSkills(dto.getRequiredSkills()))
                .build();
        return JobOfferDTO.fromEntity(jobOfferRepository.save(offer));
    }

    @Override
    public JobOfferDTO getById(Long id) {
        return JobOfferDTO.fromEntity(
                jobOfferRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Offre d'emploi introuvable : " + id)));
    }

    @Override
    public List<JobOfferDTO> getAll() {
        return jobOfferRepository.findAll()
                .stream().map(JobOfferDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<JobOfferDTO> getActive() {
        return jobOfferRepository.findByActiveTrue()
                .stream().map(JobOfferDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<JobOfferDTO> search(String keyword) {
        return jobOfferRepository.findByTitleContainingIgnoreCase(keyword)
                .stream().map(JobOfferDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public JobOfferDTO update(Long id, JobOfferDTO dto) {
        JobOffer offer = jobOfferRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Offre d'emploi introuvable : " + id));
        offer.setTitle(dto.getTitle());
        offer.setCompany(dto.getCompany());
        offer.setDescription(dto.getDescription());
        offer.setActive(dto.isActive());
        offer.setRequiredSkills(resolveSkills(dto.getRequiredSkills()));
        return JobOfferDTO.fromEntity(jobOfferRepository.save(offer));
    }

    @Override
    public void delete(Long id) {
        if (!jobOfferRepository.existsById(id))
            throw new RuntimeException("Offre d'emploi introuvable : " + id);
        jobOfferRepository.deleteById(id);
    }
}
